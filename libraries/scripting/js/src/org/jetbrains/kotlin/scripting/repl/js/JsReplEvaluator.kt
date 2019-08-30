/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import java.util.concurrent.locks.ReentrantReadWriteLock

class JsReplEvaluator : ReplEvaluator {
    //TODO: support println()
    private val engine = ScriptEngineNashorn()

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = JsState(lock)

    override fun eval(
        state: IReplStageState<*>,
        compileResult: ReplCompileResult.CompiledClasses,
        scriptArgs: ScriptArgsWithTypes?,
        invokeWrapper: InvokeWrapper?
    ): ReplEvalResult {
        return try {
            val evalResult = engine.eval<Any?>(compileResult.data as String)
            ReplEvalResult.ValueResult("result", evalResult, "Any?")
        } catch (e: Exception) {
            ReplEvalResult.Error.Runtime("Error while evaluating", e)
        }
    }
}
