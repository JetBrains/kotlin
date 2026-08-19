/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRendererWithDiagnosticId
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import java.io.File

/**
 * Routes compiler diagnostics and output items back into JPS.
 *
 * [org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] is the only hook that
 * receives structured `(severity, message, location, diagnosticId)`; a [org.jetbrains.kotlin.buildtools.api.KotlinLogger]
 * only ever sees an already rendered string, which would lose the file and line JPS needs to make messages clickable.
 * So this renderer does double duty and reports everything itself.
 *
 * Returning an empty string from [render] is load-bearing: blank renders are dropped by the Build Tools API
 * implementation, so nothing that was reported here is reported a second time through the logger.
 */
internal class JpsCompilerMessageRendererBridge(
    private val messageCollector: MessageCollector,
    private val outputItemsCollector: OutputItemsCollector,
    private val verbose: Boolean,
) : CompilerMessageRendererWithDiagnosticId {
    /**
     * How many output files the compiler reported, for the build log. Not derived from [outputItemsCollector]: that one
     * is typed as the [OutputItemsCollector] interface here, and it is shared by every module of the build.
     */
    var outputFileCount: Int = 0
        private set

    /**
     * The sources the compiler actually recompiled, which with incremental compilation is a subset of the sources the
     * operation was given. The only place that subset is observable from JPS, since the compiler expands the compile
     * set internally.
     */
    val compiledSources: MutableSet<File> = linkedSetOf()

    override fun render(
        severity: Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
        diagnosticId: String?,
    ): String {
        if (severity == Severity.OUTPUT) {
            // `-Xreport-output-files` is already set by KotlinChunk.compilerArguments
            val output = OutputMessageUtil.parseOutputMessage(message)
            val outputFile = output?.outputFile
            if (outputFile != null) {
                outputItemsCollector.add(output.sourceFiles, outputFile)
                outputFileCount++
                compiledSources.addAll(output.sourceFiles)
            }
            return ""
        }

        messageCollector.report(severity.asCompilerMessageSeverity(), message, location?.asJpsLocation())
        return ""
    }

    /**
     * [Severity] has no counterpart of [CompilerMessageSeverity.EXCEPTION]: the Build Tools API folds it into
     * [Severity.ERROR] before the renderer is called. JPS therefore loses the `INTERNAL_ERROR_PREFIX` marker it would
     * add for exceptions, but the build still fails, since both map to `BuildMessage.Kind.ERROR`.
     */
    private fun Severity.asCompilerMessageSeverity(): CompilerMessageSeverity = when (this) {
        Severity.ERROR -> CompilerMessageSeverity.ERROR
        Severity.WARNING -> CompilerMessageSeverity.WARNING
        Severity.INFO -> CompilerMessageSeverity.INFO
        // `LOGGING` is dropped by `MessageCollectorAdapter` into the build process log, which is the right place for
        // it by default and the wrong one when the consumer asked to see everything.
        Severity.DEBUG -> if (verbose) CompilerMessageSeverity.INFO else CompilerMessageSeverity.LOGGING
        Severity.OUTPUT -> CompilerMessageSeverity.OUTPUT
    }

    private fun CompilerMessageRenderer.SourceLocation.asJpsLocation() =
        CompilerMessageLocationWithRange.create(path, line, column, lineEnd, columnEnd, lineContent)
}
