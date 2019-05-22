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

object DummyLogger : Logger {
    override fun log(message: String) = println(message)
    override fun error(message: String) = println("e: $message")
    override fun warning(message: String) = println("w: $message")
    override fun fatal(message: String): Nothing {
        println("e: $message")
        exitProcess(1)
    }
}