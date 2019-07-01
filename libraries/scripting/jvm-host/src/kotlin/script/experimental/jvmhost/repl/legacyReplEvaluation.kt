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
        val history = evalState.history as ReplStageHistoryWithReplace
        val compiledScript = (compileResult.data as? KJvmCompiledScript<*>)
            ?: return ReplEvalResult.Error.CompileTime("Unable to access compiled script: ${compileResult.data}")

        val lastSnippetInstance = history.peek()?.item
        val historyBeforeSnippet = history.previousItems(compileResult.lineId)
        val currentConfiguration = ScriptEvaluationConfiguration(baseScriptEvaluationConfiguration) {
            if (historyBeforeSnippet.any()) {
                previousSnippets.put(historyBeforeSnippet.toList())
            }
            if (lastSnippetInstance != null) {
                jvm {
                    baseClassLoader(lastSnippetInstance::class.java.classLoader)
                }
            }
            if (scriptArgs != null) {
                constructorArgs(*scriptArgs.scriptArgs)
            }
        }

        val res = runBlocking { scriptEvaluator(compiledScript, currentConfiguration) }

        when (res) {
            is ResultWithDiagnostics.Success -> when (val retVal = res.value.returnValue) {
                is ResultValue.Value -> {
                    history.replaceOrPush(compileResult.lineId, retVal.scriptInstance)
                    // TODO: the latter check is temporary while the result is used to return the instance too
                    if (retVal.type.isNotBlank())
                        ReplEvalResult.ValueResult(retVal.name, retVal.value, retVal.type)
                    else
                        ReplEvalResult.UnitResult()
                }
                is ResultValue.UnitValue -> {
                    history.replaceOrPush(compileResult.lineId, retVal.scriptInstance)
                    ReplEvalResult.UnitResult()
                }
                else -> throw IllegalStateException("Expecting value with script instance, got $retVal")
            }
            else -> ReplEvalResult.Error.Runtime(res.reports.joinToString("\n") { it.message + (it.exception?.let { e -> ": $e" } ?: "") })
        }
    }
}

open class JvmReplEvaluatorState(
    scriptEvaluationConfiguration: ScriptEvaluationConfiguration,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<Any> {
    override val history: IReplStageHistory<Any> = ReplStageHistoryWithReplace(lock)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()
}

open class ReplStageHistoryWithReplace<T>(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : BasicReplStageHistory<T>(lock) {

    fun replace(id: ILineId, item: T): Boolean = lock.write {
        for (idx in indices) {
            if (get(idx).id == id) {
                set(idx, ReplHistoryRecord(id, item))
                return true
            }
        }
        return false
    }

    fun replaceOrPush(id: ILineId, item: T) {
        if (!replace(id, item)) {
            tryResetTo(id)
            push(id, item)
        }
    }

    fun previousItems(id: ILineId): Sequence<T> = asSequence().takeWhile { it.id.no < id.no }.map { it.item }
}