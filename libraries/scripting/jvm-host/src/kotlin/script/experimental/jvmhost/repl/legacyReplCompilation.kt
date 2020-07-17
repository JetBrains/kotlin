/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.common.repl.ReplCompilerWithoutCheck
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerStageHistory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.isIncomplete

/**
 * REPL Compilation wrapper for "legacy" REPL APIs defined in the org.jetbrains.kotlin.cli.common.repl package
 */
class JvmReplCompiler(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val allowReInit: Boolean = true,
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    var replCompiler: KJvmReplCompilerBase<ReplCodeAnalyzerBase> = KJvmReplCompilerBase.create(
        hostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration)
    )
) : ReplCompilerWithoutCheck {

    private val compilers = mutableListOf(replCompiler)

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
        if (allowReInit) {
            JvmReplCompilerState({ replCompiler.createReplCompilationState(it, replCompiler.initAnalyzer) }, lock)
        } else {
            replCompiler.state
        }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = replCompiler.state.lock.write {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val snippet = codeLine.toSourceCode(scriptCompilationConfiguration)

        if (allowReInit) {
            replCompiler = compilers.find { historiesEq(it.history, replCompilerState.history) } ?: {
                compilers.push(
                    KJvmReplCompilerBase.create(
                        hostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration)
                    )
                )
                compilers.last()
            }()
        }

        when (val res = runBlocking { replCompiler.compile(listOf(snippet), scriptCompilationConfiguration) }) {
            is ResultWithDiagnostics.Success -> {
                val lineId = LineId(codeLine.no, 0, snippet.hashCode())
                replCompilerState.apply {
                    lock.write {
                        val compilerHistory = history as JvmReplCompilerStageHistory<*>
                        compilerHistory.push(lineId, replCompiler.history.last().item)
                    }
                }
                ReplCompileResult.CompiledClasses(
                    lineId,
                    replCompiler.history.map { it.id },
                    snippet.name!!,
                    emptyList(),
                    res.value.get().resultField != null,
                    emptyList(),
                    res.value.get().resultField?.second?.typeName,
                    res.value
                )
            }
            else -> {
                val message = res.reports.joinToString("\n") { it.message }
                if (res.isIncomplete()) {
                    ReplCompileResult.Incomplete(message)
                } else {
                    ReplCompileResult.Error(message)
                }
            }
        }
    }

    companion object {
        fun historiesEq(history1: IReplStageHistory<*>, history2: IReplStageHistory<*>) =
            history1.count() == history2.count() &&
                    history1.zip(history2).all {
                        val (it1, it2) = it
                        it1.item === it2.item
                    }
    }
}


internal class SourceCodeFromReplCodeLine(
    val codeLine: ReplCodeLine,
    compilationConfiguration: ScriptCompilationConfiguration
) : SourceCode {
    override val text: String get() = codeLine.code
    override val name: String =
        "${compilationConfiguration[ScriptCompilationConfiguration.repl.makeSnippetIdentifier]!!(
            compilationConfiguration, ReplSnippetIdImpl(codeLine.no, codeLine.generation, 0)
        )}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
    override val locationId: String? = null
}

internal fun ReplCodeLine.toSourceCode(compilationConfiguration: ScriptCompilationConfiguration): SourceCode =
    SourceCodeFromReplCodeLine(this, compilationConfiguration)
