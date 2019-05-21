package org.jetbrains.kotlin.konan.util

import kotlin.system.exitProcess

interface Logger {
    fun log(message: String)
    fun error(message: String)
    fun warning(message: String)
    fun fatal(message: String): Nothing
}

interface WithLogger {
    val logger: Logger
}

fun dummyLogger(message: String) {}

object DummyLogger : Logger {
    override fun log(message: String) = dummyLogger(message)
    override fun error(message: String) = dummyLogger(message)
    override fun warning(message: String) = dummyLogger(message)
    override fun fatal(message: String): Nothing {
        dummyLogger(message)
        exitProcess(1)
    }
}