package util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun currentTime(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return current.format(formatter)
}

fun String.purple() = "\u001B[35m$this\u001B[0m"

fun String.red() = "\u001B[31m$this\u001B[0m"

fun String.blue() = "\u001B[34m$this\u001B[0m"
