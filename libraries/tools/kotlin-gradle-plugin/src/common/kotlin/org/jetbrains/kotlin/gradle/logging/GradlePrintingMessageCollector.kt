/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GradleStyleMessageRenderer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal class GradlePrintingMessageCollector(
    val logger: KotlinLogger,
    private val allWarningsAsErrors: Boolean
) :
    MessageCollector {
    constructor(logger: Logger, allWarningsAsErrors: Boolean) : this(GradleKotlinLogger(logger), allWarningsAsErrors)

    private var hasErrors = false

    private val messageRenderer = GradleStyleMessageRenderer()

    override fun hasErrors() = hasErrors

    override fun clear() {
        // Do nothing
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val renderedMessage = messageRenderer.render(severity, message, location)

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        when (severity) {
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.EXCEPTION -> {
                hasErrors = true
                logger.error(renderedMessage)
            }

            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.STRONG_WARNING -> {
                if (allWarningsAsErrors) {
                    logger.error(renderedMessage)
                } else {
                    logger.warn(renderedMessage)
                }
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
