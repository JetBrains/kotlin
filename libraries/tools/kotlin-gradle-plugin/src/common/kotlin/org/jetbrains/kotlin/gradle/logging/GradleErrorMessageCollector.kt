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
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.io.File

class GradleErrorMessageCollector(
    val logger: KotlinLogger,
    private val delegate: MessageCollector? = null,
    private val acceptableMessageSeverity: List<CompilerMessageSeverity> = listOf(CompilerMessageSeverity.EXCEPTION),
    val kotlinPluginVersion: String? = null,
) : MessageCollector {
    private val buildErrorMessageCollector = BuildErrorMessageCollector(logger, kotlinPluginVersion)

    constructor(
        logger: Logger,
        delegate: MessageCollector? = null,
        acceptableMessageSeverity: List<CompilerMessageSeverity> = listOf(CompilerMessageSeverity.EXCEPTION),
        kotlinPluginVersion: String? = getKotlinPluginVersion(logger),
    ) : this(GradleKotlinLogger(logger), delegate, acceptableMessageSeverity, kotlinPluginVersion)

    override fun clear() {
        delegate?.clear()
    }

    fun report(error: Throwable, location: CompilerMessageSourceLocation?) {
        report(CompilerMessageSeverity.EXCEPTION, "${error.message}\n${error.stackTraceToString()}", location)
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        delegate?.report(severity, message, location)

        if (severity in acceptableMessageSeverity) {
            buildErrorMessageCollector.addError(message)
        }
    }

    override fun hasErrors(): Boolean {
        return buildErrorMessageCollector.hasErrors()
    }

    fun flush(files: Set<File>) {
        buildErrorMessageCollector.flush(files)
        clear()
    }
}