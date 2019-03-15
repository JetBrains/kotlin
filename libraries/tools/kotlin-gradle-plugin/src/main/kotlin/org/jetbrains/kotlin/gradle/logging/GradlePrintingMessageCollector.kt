/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GradleStyleMessageRenderer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinLogger

internal class GradlePrintingMessageCollector(val logger: KotlinLogger) :
    MessageCollector {
    constructor(logger: Logger) : this(GradleKotlinLogger(logger))

    private var hasErrors = false

    private val messageRenderer = GradleStyleMessageRenderer()

    override fun hasErrors() = hasErrors

    override fun clear() {
        // Do nothing
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        val renderedMessage = messageRenderer.render(severity, message, location)

        when (severity) {
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.EXCEPTION -> {
                hasErrors = true
                logger.error(renderedMessage)
            }

            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.STRONG_WARNING -> {
                logger.warn(renderedMessage)
            }
            CompilerMessageSeverity.INFO -> {
                logger.info(renderedMessage)
            }
            CompilerMessageSeverity.LOGGING,
            CompilerMessageSeverity.OUTPUT -> {
                logger.debug(renderedMessage)
            }
        }!! // !! is used to force compile-time exhaustiveness
    }
}