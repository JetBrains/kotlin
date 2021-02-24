/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageParserCallback
import jetbrains.buildServer.messages.serviceMessages.TestFailed
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler.Companion.MESSAGE_LIMIT_BYTES
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Provides output stream that handles lines and parsing it for TeamCity server messages.
 * Calls [client] for each parsed message and regular text.
 *
 * TeamCity server messages should ends with new line.
 * Only messages short than [MESSAGE_LIMIT_BYTES] supported.
 */
internal class TCServiceMessageOutputStreamHandler(
    private val client: ServiceMessageParserCallback,
    private val onException: () -> Unit,
    private val logger: Logger,
    private val ignoreTcsmOverflow: Boolean = false,
    private val messageLimitBytes: Int = MESSAGE_LIMIT_BYTES // for test only
) : OutputStream() {
    private var closed: Boolean = false
    private val buffer = ByteArrayOutputStream()
    private var overflowInsideMessage: Boolean = false

    @Throws(IOException::class)
    override fun close() {
        closed = true
        flushLine()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (closed) throw IOException("The stream has been closed.")
        var i = off
        var last = off

        fun bytesToAppend() =
            i - last

        val end = off + len

        fun append(len: Int = bytesToAppend()) {
            buffer.write(b, last, i - last)
            last += len
        }

        while (i < end) {
            val c = b[i++]
            if (c == '\n'.toByte()) {
                append()
                flushLine()
            } else if (buffer.size() + bytesToAppend() >= messageLimitBytes) {
                append(messageLimitBytes - buffer.size())
                overflow()
            }
        }

        append()
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        // helpfully will be inlined at runtime
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    private fun flushLine() {
        overflowInsideMessage = false
        if (buffer.size() > 0) {
            val text = buffer.toString("utf-8")
            parse(text)
            buffer.reset()
        }
    }

    private fun overflow() {
        val text = buffer.toString("utf-8")

        // support messageLimitBytes inside "##teamcity[...]" (including "##teamcity[]").
        val i = if (overflowInsideMessage) {
            if (!ignoreTcsmOverflow && !text.endsWith("]")) {
                logger.warn(text)
                overflowInsideMessage = false
                buffer.reset()
                client.serviceMessage(
                    TestFailed(
                        "overflow-message",
                        GradleException(
                            """
                            Cannot process output: too long teamcity service message (more than ${MESSAGE_LIMIT_BYTES / 1024 / 1024}Mb). Event was lost.
                            Build failed to prevent inconsistent behaviour. To ignore it use Gradle property '$IGNORE_TCSM_OVERFLOW=true'
                            """.trimIndent()
                        )
                    )
                )
            }
            -1
        } else text.indexOf("##teamcity[")
        if (i != -1) {
            client.regularText(text.substring(0, i))
            buffer.reset()
            buffer.write(text.substring(i).toByteArray())
            overflowInsideMessage = true
        } else {
            flushLine()
        }
    }

    private fun parse(text: String) {
        try {
            ServiceMessage.parse(text, client)
        } catch (e: Exception) {
            onException()
            logger.error(
                "Error while processing test process output message \"$text\"",
                e
            )
        }
    }

    companion object {
        private const val MESSAGE_LIMIT_BYTES = 1024 * 1024 // 1Mb
        const val IGNORE_TCSM_OVERFLOW = "kotlin.ignore.tcsm.overflow"
    }
}
