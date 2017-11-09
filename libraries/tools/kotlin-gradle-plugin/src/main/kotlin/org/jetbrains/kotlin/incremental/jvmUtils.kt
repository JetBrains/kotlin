package org.jetbrains.kotlin.incremental

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.PrintStream

internal val Throwable.stackTraceStr: String
    get() {
        val byteOutputStream = ByteOutputStream()
        val printStream = PrintStream(byteOutputStream)
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (this as? java.lang.Throwable)?.printStackTrace(printStream)
        return byteOutputStream.toString()
    }