/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.util.*

internal class GradleBufferingMessageCollector : MessageCollector {
    private class MessageData(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    )

    private val messages = ArrayList<MessageData>()

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        synchronized(messages) {
            messages.add(MessageData(severity, message, location))
        }
    }

    override fun hasErrors() =
        synchronized(messages) {
            messages.any { it.severity.isError }
        }

    override fun clear() {
        synchronized(messages) {
            messages.clear()
        }
    }

    fun flush(delegate: MessageCollector) {
        messages.forEach {
            delegate.report(it.severity, it.message, it.location)
        }
        clear()
    }
}