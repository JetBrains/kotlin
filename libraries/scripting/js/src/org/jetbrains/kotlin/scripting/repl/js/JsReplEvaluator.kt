/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import java.util.concurrent.locks.ReentrantReadWriteLock

class JsReplEvaluator : ReplEvaluator {
    //TODO: support println()
    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = JsEvaluationState(lock, ScriptEngineNashorn())

    override fun eval(
        state: IReplStageState<*>,
        compileResult: ReplCompileResult.CompiledClasses,
        scriptArgs: ScriptArgsWithTypes?,
        invokeWrapper: InvokeWrapper?
    ): ReplEvalResult {
        return try {
            val evaluationState = state.asState(JsEvaluationState::class.java)
            val evalResult = evaluationState.engine.evalWithTypedResult<Any?>(compileResult.data as String)
            ReplEvalResult.ValueResult("result", evalResult, "Any?")
        } catch (e: Exception) {
            ReplEvalResult.Error.Runtime("Error while evaluating", e)
        }
    }
}
