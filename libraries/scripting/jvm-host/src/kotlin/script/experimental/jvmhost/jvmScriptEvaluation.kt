/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

open class JvmScriptEvaluationConfiguration : PropertiesCollection.Builder() {

    companion object : JvmScriptEvaluationConfiguration()
}

val JvmScriptEvaluationConfiguration.baseClassLoader by PropertiesCollection.key<ClassLoader?>(Thread.currentThread().contextClassLoader)

val ScriptEvaluationConfiguration.jvm get() = JvmScriptEvaluationConfiguration()

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> =
        try {
            val res = compiledScript.getClass(scriptEvaluationConfiguration)
            when (res) {
                is ResultWithDiagnostics.Failure -> res
                is ResultWithDiagnostics.Success -> {
                    // in the future, when (if) we'll stop to compile everything into constructor
                    // run as SAM
                    // return res
                    val scriptClass = res.value
                    val args = ArrayList<Any?>()
                    scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.providedProperties)?.forEach {
                        args.add(it.value)
                    }
                    scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.implicitReceivers)?.let {
                        args.addAll(it)
                    }
                    scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.constructorArgs)?.let {
                        args.addAll(it)
                    }
                    val ctor = scriptClass.java.constructors.single()
                    val instance = ctor.newInstance(*args.toArray())

                    // TODO: fix result value
                    ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Value("", instance, ""), scriptEvaluationConfiguration))
                }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics("Error evaluating script"))
        }
}
