/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File
import java.io.FileWriter

class GradleErrorMessageCollector(
    private val logger: KotlinLogger,
    private val delegate: MessageCollector? = null,
    private val acceptableMessageSeverity: List<CompilerMessageSeverity> = listOf(CompilerMessageSeverity.EXCEPTION),
    private val kotlinPluginVersion: String? = null
) : MessageCollector {

    constructor(
        logger: Logger,
        delegate: MessageCollector? = null,
        acceptableMessageSeverity: List<CompilerMessageSeverity> = listOf(CompilerMessageSeverity.EXCEPTION),
        kotlinPluginVersion: String? = null,
    ) : this(GradleKotlinLogger(logger), delegate, acceptableMessageSeverity, kotlinPluginVersion)

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

    fun flush(files: Set<File>) {
        if (!hasErrors()) {
            return
        }
        for (file in files) {
            file.parentFile.mkdirs()
            file.createNewFile()
            FileWriter(file).use {
                kotlinPluginVersion?.also { version -> it.append("kotlin version: $version\n") }
                for (error in errors) {
                    it.append("error message: $error\n\n")
                }
                it.flush()
            }
            logger.debug("${errors.count()} errors were stored into file ${file.absolutePath}")
        }
        clear()
    }
}