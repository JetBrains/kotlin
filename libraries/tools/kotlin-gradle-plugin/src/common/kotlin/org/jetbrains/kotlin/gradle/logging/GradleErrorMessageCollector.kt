/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File
import java.io.FileWriter

class GradleErrorMessageCollector(
    private val delegate: MessageCollector? = null,
    private val acceptableMessageSeverity: List<CompilerMessageSeverity> = listOf(CompilerMessageSeverity.EXCEPTION),
    private val kotlinPluginVersion: String? = null
) : MessageCollector {

    private val errors = ArrayList<String>()

    override fun clear() {
        delegate?.clear()
        errors.clear()
    }

    fun report(error: Throwable, location: CompilerMessageSourceLocation?) {
        report(CompilerMessageSeverity.EXCEPTION, "${error.message}\n${error.stackTraceToString()}", location)
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        delegate?.report(severity, message, location)

        if (severity in acceptableMessageSeverity) {
            synchronized(errors) {
                errors.add(message)
            }
        }
    }

    override fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun flush(file: File) {
        if (!hasErrors()) {
            return
        }
        file.createNewFile()
        println("Errors were stored into ${file.absolutePath}")
        FileWriter(file).use {
            kotlinPluginVersion?.also { version -> it.append("kotlin version: $version\n") }
            for (error in errors) {
                it.append("error message: $error\n\n")
            }
            it.flush()
        }
        clear()
    }
}