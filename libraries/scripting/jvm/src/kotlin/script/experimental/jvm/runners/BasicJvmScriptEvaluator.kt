/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.runners

import kotlin.script.experimental.api.*

open class BasicJvmScriptEvaluator<ScriptBase : Any>(val environment: ScriptingEnvironment) : ScriptEvaluator<ScriptBase> {

    override suspend fun eval(
        compiledScript: CompiledScript<ScriptBase>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult> =
        try {
            val obj = compiledScript.instantiate(scriptEvaluationEnvironment)
            when (obj) {
                is ResultWithDiagnostics.Failure -> obj
                is ResultWithDiagnostics.Success -> {
                    // in the future, when (if) we'll stop to compile everything into constructor
                    // run as SAM
                    // return res
                    val scriptObject = obj.value
                    if (scriptObject !is Class<*>)
                        ResultWithDiagnostics.Failure(ScriptDiagnostic("expecting class in this implementation, got ${scriptObject?.javaClass}"))
                    else {
                        val receivers = scriptEvaluationEnvironment.getOrNull(ScriptEvaluationEnvironmentParams.implicitReceivers)
                        if (receivers == null) {
                            scriptObject.getConstructor().newInstance()
                        } else {
                            scriptObject.getConstructor(Array<Any>::class.java).newInstance(receivers.toTypedArray())
                        }

                        ResultWithDiagnostics.Success(EvaluationResult(null, scriptEvaluationEnvironment))
                    }
                }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics())
        }
}
