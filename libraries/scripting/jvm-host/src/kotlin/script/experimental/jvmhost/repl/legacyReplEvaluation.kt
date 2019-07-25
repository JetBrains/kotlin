/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.repl.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.reflect.KClass
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

        val lastSnippetClass = history.peek()?.item?.first
        val historyBeforeSnippet = history.previousItems(compileResult.lineId).map { it.second }.toList()
        val currentConfiguration = ScriptEvaluationConfiguration(baseScriptEvaluationConfiguration) {
            if (historyBeforeSnippet.isNotEmpty()) {
                previousSnippets.put(historyBeforeSnippet)
            }
            if (lastSnippetClass != null) {
                jvm {
                    baseClassLoader(lastSnippetClass.java.classLoader)
                }
            }
            if (scriptArgs != null) {
                constructorArgs(*scriptArgs.scriptArgs)
            }
        }

        val res = runBlocking { scriptEvaluator(compiledScript, currentConfiguration) }

        when (res) {
            is ResultWithDiagnostics.Success -> {
                when (val retVal = res.value.returnValue) {
                    is ResultValue.Error -> {
                        history.replaceOrPush(compileResult.lineId, retVal.scriptClass to null)
                        ReplEvalResult.Error.Runtime(
                            retVal.error.message ?: "unknown error",
                            (retVal.error as? Exception) ?: (retVal.wrappingException as? Exception)
                        )
                    }
                    is ResultValue.Value -> {
                        history.replaceOrPush(compileResult.lineId, retVal.scriptClass to retVal.scriptInstance)
                        ReplEvalResult.ValueResult(retVal.name, retVal.value, retVal.type)
                    }
                    is ResultValue.Unit -> {
                        history.replaceOrPush(compileResult.lineId, retVal.scriptClass to retVal.scriptInstance)
                        ReplEvalResult.UnitResult()
                    }
                    else -> throw IllegalStateException("Unexpected snippet result value $retVal")
                }
            }
            else ->
                ReplEvalResult.Error.Runtime(
                    res.reports.joinToString("\n") { it.message + (it.exception?.let { e -> ": $e" } ?: "") },
                    res.reports.find { it.exception != null }?.exception as? Exception
                )
        }
    }
}

open class JvmReplEvaluatorState(
    scriptEvaluationConfiguration: ScriptEvaluationConfiguration,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<Pair<KClass<*>?, Any?>> {
    override val history: IReplStageHistory<Pair<KClass<*>?, Any?>> = ReplStageHistoryWithReplace(lock)

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