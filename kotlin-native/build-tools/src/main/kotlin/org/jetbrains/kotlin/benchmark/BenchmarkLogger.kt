package org.jetbrains.kotlin.benchmark

import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel { DEBUG, OFF }

class Logger(val level: LogLevel = LogLevel.OFF) {
    private fun printStderr(message: String) {
        System.err.print(message)
    }

    private fun currentTime(): String =
            SimpleDateFormat("HH:mm:ss").format(Date())

    fun log(message: String, messageLevel: LogLevel = LogLevel.DEBUG, usePrefix: Boolean = true) {
        if (messageLevel == level) {
            if (usePrefix) {
                printStderr("[$level][${currentTime()}] $message")
            } else {
                printStderr("$message")
            }
        }
    }
}