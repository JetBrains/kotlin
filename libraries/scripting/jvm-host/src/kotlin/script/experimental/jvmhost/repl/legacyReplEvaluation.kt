/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.repl.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm

/**
 * REPL Evaluation wrapper for "legacy" REPL APIs defined in the org.jetbrains.kotlin.cli.common.repl package
 */
class JvmReplEvaluator(
    val baseScriptEvaluationConfiguration: ScriptEvaluationConfiguration,
    val scriptEvaluator: ScriptEvaluator = BasicJvmScriptEvaluator()
) : ReplEvaluator {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
        JvmReplEvaluatorState(baseScriptEvaluationConfiguration, lock)

    override fun eval(
        state: IReplStageState<*>,
        compileResult: ReplCompileResult.CompiledClasses,
        scriptArgs: ScriptArgsWithTypes?,
        invokeWrapper: InvokeWrapper?
    ): ReplEvalResult = state.lock.write {
        val evalState = state.asState(JvmReplEvaluatorState::class.java)
        val compiledScript = (compileResult.data as? KJvmCompiledScript<*>)
            ?: return ReplEvalResult.Error.CompileTime("Unable to access compiled script: ${compileResult.data}")

        val lastSnippetInstance = evalState.history.peek()?.item
        val currentConfiguration = ScriptEvaluationConfiguration(baseScriptEvaluationConfiguration) {
            if (evalState.history.isNotEmpty()) {
                previousSnippets.put(evalState.history.map { it.item })
            }
            if (lastSnippetInstance != null) {
                jvm {
                    baseClassLoader(lastSnippetInstance::class.java.classLoader)
                }
            }
        }

        val res = runBlocking { scriptEvaluator(compiledScript, currentConfiguration) }

        when (res) {
            is ResultWithDiagnostics.Success -> when (val retVal = res.value.returnValue) {
                is ResultValue.Value -> {
                    evalState.history.push(compileResult.lineId, retVal.scriptInstance)
                    // TODO: the latter check is temporary while the result is used to return the instance too
                    if (retVal.type.isNotBlank())
                        ReplEvalResult.ValueResult(retVal.name, retVal.value, retVal.type)
                    else
                        ReplEvalResult.UnitResult()
                }
                is ResultValue.UnitValue -> {
                    evalState.history.push(compileResult.lineId, retVal.scriptInstance)
                    ReplEvalResult.UnitResult()
                }
                else -> throw IllegalStateException("Expecting value with script instance, got $retVal")
            }
            else -> ReplEvalResult.Error.Runtime(res.reports.joinToString("\n") { it.message })
        }
    }
}

open class JvmReplEvaluatorState(
    scriptEvaluationConfiguration: ScriptEvaluationConfiguration,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<Any> {

    override val history: IReplStageHistory<Any> = BasicReplStageHistory(lock)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()

    val topClassLoader: ClassLoader = scriptEvaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]!!
}
