/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptEvaluationConfigurationKeys

open class JvmScriptEvaluationConfigurationBuilder : PropertiesCollection.Builder(), JvmScriptEvaluationConfigurationKeys {

    companion object : JvmScriptEvaluationConfigurationBuilder()
}

val JvmScriptEvaluationConfigurationKeys.baseClassLoader by PropertiesCollection.key<ClassLoader?>(Thread.currentThread().contextClassLoader)

val JvmScriptEvaluationConfigurationKeys.actualClassLoader by PropertiesCollection.key<ClassLoader?>()

val ScriptEvaluationConfigurationKeys.jvm get() = JvmScriptEvaluationConfigurationBuilder()

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> =
        try {
            val actualEvalConfiguration = scriptEvaluationConfiguration ?: ScriptEvaluationConfiguration()
            compiledScript.getClass(actualEvalConfiguration).onSuccess { scriptClass ->
                // in the future, when (if) we'll stop to compile everything into constructor
                // run as SAM
                // return res

                // for other scripts we need evaluation configuration with actualClassloader set,
                // so they are loaded in the same classloader as the "main" script
                val updatedEvalConfiguration =
                    if (actualEvalConfiguration.containsKey(ScriptEvaluationConfiguration.jvm.actualClassLoader))
                        actualEvalConfiguration
                    else
                        ScriptEvaluationConfiguration(actualEvalConfiguration) {
                            ScriptEvaluationConfiguration.jvm.actualClassLoader(scriptClass.java.classLoader)
                        }

                val sharedScripts = actualEvalConfiguration[ScriptEvaluationConfiguration.scriptsInstancesSharingMap]

                sharedScripts?.get(scriptClass)?.asSuccess()
                    ?: compiledScript.otherScripts.mapSuccess {
                        invoke(it, updatedEvalConfiguration)
                    }.onSuccess { importedScriptsEvalResults ->

                        val refinedEvalConfiguration =
                            updatedEvalConfiguration[ScriptEvaluationConfiguration.refineConfigurationBeforeEvaluate]
                                ?.handler?.invoke(ScriptEvaluationConfigurationRefinementContext(compiledScript, updatedEvalConfiguration))
                                ?.onFailure {
                                    return@invoke ResultWithDiagnostics.Failure(it.reports)
                                }
                                ?.resultOrNull()
                                ?: updatedEvalConfiguration

                        scriptClass.evalWithConfigAndOtherScriptsResults(refinedEvalConfiguration, importedScriptsEvalResults).let {
                            sharedScripts?.put(scriptClass, it)
                            ResultWithDiagnostics.Success(it)
                        }
                    }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics("Error evaluating script", path = compiledScript.sourceLocationId))
        }

    private fun KClass<*>.evalWithConfigAndOtherScriptsResults(
        refinedEvalConfiguration: ScriptEvaluationConfiguration,
        importedScriptsEvalResults: List<EvaluationResult>
    ): EvaluationResult {
        val args = ArrayList<Any?>()

        refinedEvalConfiguration[ScriptEvaluationConfiguration.constructorArgs]?.let {
            args.addAll(it)
        }
        refinedEvalConfiguration[ScriptEvaluationConfiguration.implicitReceivers]?.let {
            args.addAll(it)
        }
        refinedEvalConfiguration[ScriptEvaluationConfiguration.providedProperties]?.forEach {
            args.add(it.value)
        }

        importedScriptsEvalResults.forEach {
            args.add((it.returnValue as ResultValue.Value).scriptInstance)
        }

        val ctor = java.constructors.single()
        val instance = ctor.newInstance(*args.toArray())

        return EvaluationResult(ResultValue.Value("", instance, "", instance), refinedEvalConfiguration)
    }
}

