/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerRunnerConstants
import org.jetbrains.kotlin.jps.platforms.KotlinModuleBuildTarget
import java.io.File

class MessageCollectorAdapter(
    private val context: CompileContext,
    private val kotlinTarget: KotlinModuleBuildTarget<*>?
) : MessageCollector {
    private var hasErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        hasErrors = hasErrors || severity.isError

        var prefix = ""
        if (severity == CompilerMessageSeverity.EXCEPTION) {
            prefix = CompilerRunnerConstants.INTERNAL_ERROR_PREFIX
        }

        val kind = kind(severity)
        if (kind != null) {
            // Report target when cross-compiling common files
            if (location != null && kotlinTarget != null && kotlinTarget.isCommonModuleFile(File(location.path))) {
                val moduleName = kotlinTarget.module.name
                prefix += "[$moduleName] "
            }

            context.processMessage(
                CompilerMessage(
                    CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                    kind,
                    prefix + message,
                    location?.path,
                    -1, -1, -1,
                    location?.line?.toLong() ?: -1,
                    location?.column?.toLong() ?: -1
                )
            )
        } else {
            val path = if (location != null) "${location.path}:${location.line}:${location.column}: " else ""
            KotlinBuilder.LOG.debug(path + message)
        }
    }

    override fun clear() {
        hasErrors = false
    }

    override fun hasErrors(): Boolean = hasErrors

    private fun kind(severity: CompilerMessageSeverity): BuildMessage.Kind? {
        return when (severity) {
            CompilerMessageSeverity.INFO -> BuildMessage.Kind.INFO
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> BuildMessage.Kind.ERROR
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> BuildMessage.Kind.WARNING
            CompilerMessageSeverity.LOGGING -> null
            else -> throw IllegalArgumentException("Unsupported severity: $severity")
        }
    }
}