/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.typedKey

object JvmScriptEvaluationEnvironmentProperties {
    val baseClassLoader by typedKey<ClassLoader?>(Thread.currentThread().contextClassLoader)
}

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
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
                        val instance = if (receivers == null) {
                            scriptObject.getConstructor().newInstance()
                        } else {
                            scriptObject.getConstructor(Array<Any>::class.java).newInstance(receivers.toTypedArray())
                        }

                        // TODO: fix result value
                        ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Value("", instance, ""), scriptEvaluationEnvironment))
                    }
                }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics())
        }
}
