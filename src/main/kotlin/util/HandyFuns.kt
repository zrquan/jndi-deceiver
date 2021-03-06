package util

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*

fun String.purple() = "\u001B[35m$this\u001B[0m"
fun String.red() = "\u001B[31m$this\u001B[0m"
fun String.blue() = "\u001B[34m$this\u001B[0m"
fun String.green() = "\u001B[32m$this\u001B[0m"

fun base46cmd(cmd: String) =
    "bash -c {echo,${
        Base64.getEncoder().encodeToString(cmd.toByteArray())
    }}|{base64,-d}|{bash,-i}"

fun serialize(obj: Any): ByteArray {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(obj)
    return baos.toByteArray()
}

fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this)
