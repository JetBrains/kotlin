/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.isIncomplete

/**
 * REPL Compilation wrapper for "legacy" REPL APIs defined in the org.jetbrains.kotlin.cli.common.repl package
 */
class JvmReplCompiler(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration
) : ReplCompilerWithoutCheck {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
        JvmReplCompilerState({ KJvmReplCompilerBase.createCompilationState(it, hostConfiguration) }, lock)

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = state.lock.write {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val snippet = codeLine.toSourceCode(scriptCompilationConfiguration)

        val replCompiler = KJvmReplCompilerBase<ReplCodeAnalyzerBase>(
            hostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration),
            replCompilerState
        )

        @Suppress("DEPRECATION_ERROR")
        when (val res = internalScriptingRunSuspend { replCompiler.compile(listOf(snippet), scriptCompilationConfiguration) }) {
            is ResultWithDiagnostics.Success -> {
                val lineId = LineId(codeLine.no, 0, snippet.hashCode())
                ReplCompileResult.CompiledClasses(
                    lineId,
                    replCompiler.state.history.map { it.id },
                    snippet.name!!,
                    emptyList(),
                    res.value.get().resultField != null,
                    emptyList(),
                    res.value.get().resultField?.second?.typeName,
                    res.value
                )
            }
            else -> {
                val message = res.reports.joinToString("\n")
                if (res.isIncomplete()) {
                    ReplCompileResult.Incomplete(message)
                } else {
                    ReplCompileResult.Error(message)
                }
            }
        }
    }
}


internal class SourceCodeFromReplCodeLine(
    val codeLine: ReplCodeLine,
    compilationConfiguration: ScriptCompilationConfiguration
) : SourceCode {
    override val text: String get() = codeLine.code
    override val name: String =
        "${
            compilationConfiguration[ScriptCompilationConfiguration.repl.makeSnippetIdentifier]!!(
                compilationConfiguration, ReplSnippetIdImpl(codeLine.no, codeLine.generation, 0)
            )
        }.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
    override val locationId: String? = null
}

internal fun ReplCodeLine.toSourceCode(compilationConfiguration: ScriptCompilationConfiguration): SourceCode =
    SourceCodeFromReplCodeLine(this, compilationConfiguration)
