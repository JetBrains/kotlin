/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.kapt.KaptCliDiagnostics
import org.jetbrains.kotlin.kapt.base.KaptFlag
import org.jetbrains.kotlin.kapt.base.KaptFlags
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import java.io.PrintWriter
import java.io.StringWriter

class CompilerConfigurationBackedKaptLogger(
    override val isVerbose: Boolean,
    isInfoAsWarnings: Boolean,
    val configuration: CompilerConfiguration,
) : KaptLogger {
    constructor(flags: KaptFlags, configuration: CompilerConfiguration)
            : this(flags[KaptFlag.VERBOSE], flags[KaptFlag.INFO_AS_WARNINGS], configuration)

    private companion object {
        const val PREFIX = "[kapt] "
    }

    override val errorWriter = makeWriter(KaptCliDiagnostics.KAPT_ERROR)
    override val warnWriter = makeWriter(KaptCliDiagnostics.KAPT_STRONG_WARNING)
    override val infoWriter = makeWriter(if (isInfoAsWarnings) KaptCliDiagnostics.KAPT_WARNING else KaptCliDiagnostics.KAPT_INFO)

    override fun info(message: String) {
        if (isVerbose) {
            configuration.report(KaptCliDiagnostics.KAPT_INFO, PREFIX + message)
        }
    }

    override fun warn(message: String) {
        configuration.report(KaptCliDiagnostics.KAPT_STRONG_WARNING, PREFIX + message)
    }

    override fun error(message: String) {
        configuration.report(KaptCliDiagnostics.KAPT_ERROR, PREFIX + message)
    }

    override fun exception(e: Throwable) {
        val stacktrace = run {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            writer.toString()
        }
        configuration.report(KaptCliDiagnostics.KAPT_ERROR, PREFIX + "An exception occurred: " + stacktrace)
    }

    private fun makeWriter(factory: KtSourcelessDiagnosticFactory): PrintWriter {
        return PrintWriter(CallbackBasedWriter { configuration.report(factory, it) })
    }
}
