package org.jetbrains.kotlin.konan.util

typealias Logger = (String) -> Unit

interface WithLogger {
    val logger: Logger
}

fun dummyLogger(message: String) {}

