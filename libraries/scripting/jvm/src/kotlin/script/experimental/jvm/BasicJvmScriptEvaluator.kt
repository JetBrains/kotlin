/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.impl.getConfigurationWithClassloader

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> = try {
        val configuration = getConfigurationWithClassloader(compiledScript, scriptEvaluationConfiguration)

        compiledScript.getClass(configuration).onSuccess { scriptClass ->
            // in the future, when (if) we'll stop to compile everything into constructor
            // run as SAM
            // return res

            val sharedScripts = configuration[ScriptEvaluationConfiguration.jvm.scriptsInstancesSharingMap]

            sharedScripts?.get(scriptClass)?.asSuccess()
                ?: compiledScript.otherScripts.mapSuccess {
                    invoke(it, configuration)
                }.onSuccess { importedScriptsEvalResults ->

                    val refinedEvalConfiguration =
                        configuration[ScriptEvaluationConfiguration.refineConfigurationBeforeEvaluate]
                            ?.handler?.invoke(
                            ScriptEvaluationConfigurationRefinementContext(
                                compiledScript,
                                configuration
                            )
                        )
                            ?.onFailure {
                                return@invoke ResultWithDiagnostics.Failure(it.reports)
                            }
                            ?.resultOrNull()
                            ?: configuration

                    val instance =
                        scriptClass.evalWithConfigAndOtherScriptsResults(refinedEvalConfiguration, importedScriptsEvalResults)

                    val resultValue = compiledScript.resultField?.let { (resultFieldName, resultType) ->
                        val resultField = scriptClass.java.getDeclaredField(resultFieldName).apply { isAccessible = true }
                        ResultValue.Value(resultFieldName, resultField.get(instance), resultType.typeName, instance)
                    } ?: ResultValue.Value("", instance, "", instance)

                    EvaluationResult(resultValue, refinedEvalConfiguration).let {
                        sharedScripts?.put(scriptClass, it)
                        ResultWithDiagnostics.Success(it)
                    }
                }
        }
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(
            e.asDiagnostics(
                "Error evaluating script",
                path = compiledScript.sourceLocationId
            )
        )
    }

    private fun KClass<*>.evalWithConfigAndOtherScriptsResults(
        refinedEvalConfiguration: ScriptEvaluationConfiguration,
        importedScriptsEvalResults: List<EvaluationResult>
    ): Any {
        val args = ArrayList<Any?>()

        refinedEvalConfiguration[ScriptEvaluationConfiguration.previousSnippets]?.let {
            if (it.isNotEmpty()) {
                args.add(it.toTypedArray())
            }
        }

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

        return instance
    }
}

