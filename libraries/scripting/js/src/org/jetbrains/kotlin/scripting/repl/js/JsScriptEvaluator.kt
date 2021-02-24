/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import kotlin.script.experimental.api.*

class JsScriptEvaluator : ScriptEvaluator {
    //TODO: support println()
    private val engine = ScriptEngineNashorn()

    override suspend fun invoke(
        compiledScript: CompiledScript,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> {
        return try {
            val evalResult = engine.evalWithTypedResult<Any?>((compiledScript as JsCompiledScript).jsCode)
            ResultWithDiagnostics.Success(
                EvaluationResult(
                    ResultValue.Value(
                        name = "result",
                        value = evalResult,
                        type = "Any?",
                        scriptClass = null,
                        scriptInstance = null
                    ),
                    scriptEvaluationConfiguration
                )
            )
        } catch (e: Exception) {
            ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    message = e.localizedMessage,
                    severity = ScriptDiagnostic.Severity.ERROR,
                    exception = e
                )
            )
        }
    }
}
