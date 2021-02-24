/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> = try {
        compiledScript.getClass(scriptEvaluationConfiguration).onSuccess { scriptClass ->

            // configuration shared between all module scripts
            val sharedConfiguration = scriptEvaluationConfiguration.getOrPrepareShared(scriptClass.java.classLoader)

            val sharedScripts = sharedConfiguration[ScriptEvaluationConfiguration.jvm.scriptsInstancesSharingMap]

            sharedScripts?.get(scriptClass)?.asSuccess()
                ?: compiledScript.otherScripts.mapSuccess {
                    invoke(it, sharedConfiguration)
                }.onSuccess { importedScriptsEvalResults ->

                    val refinedEvalConfiguration =
                        sharedConfiguration.refineBeforeEvaluation(compiledScript).valueOr {
                            return@invoke ResultWithDiagnostics.Failure(it.reports)
                        }

                    val resultValue = try {
                        // in the future, when (if) we'll stop to compile everything into constructor
                        // run as SAM
                        // return res

                        val instance =
                            scriptClass.evalWithConfigAndOtherScriptsResults(refinedEvalConfiguration, importedScriptsEvalResults)

                        compiledScript.resultField?.let { (resultFieldName, resultType) ->
                            val resultField = scriptClass.java.getDeclaredField(resultFieldName).apply { isAccessible = true }
                            ResultValue.Value(resultFieldName, resultField.get(instance), resultType.typeName, scriptClass, instance)
                        } ?: ResultValue.Unit(scriptClass, instance)

                    } catch (e: InvocationTargetException) {
                        ResultValue.Error(e.targetException ?: e, e, scriptClass)
                    }

                    EvaluationResult(resultValue, refinedEvalConfiguration).let {
                        sharedScripts?.put(scriptClass, it)
                        ResultWithDiagnostics.Success(it)
                    }
                }
        }
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(
            e.asDiagnostics(path = compiledScript.sourceLocationId)
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

        importedScriptsEvalResults.forEach {
            args.add(it.returnValue.scriptInstance)
        }

        refinedEvalConfiguration[ScriptEvaluationConfiguration.implicitReceivers]?.let {
            args.addAll(it)
        }
        refinedEvalConfiguration[ScriptEvaluationConfiguration.providedProperties]?.forEach {
            args.add(it.value)
        }

        val ctor = java.constructors.single()

        val saveClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this.java.classLoader
        return try {
            ctor.newInstance(*args.toArray())
        } finally {
            Thread.currentThread().contextClassLoader = saveClassLoader
        }
    }
}

private fun ScriptEvaluationConfiguration.getOrPrepareShared(classLoader: ClassLoader): ScriptEvaluationConfiguration =
    if (this[ScriptEvaluationConfiguration.jvm.actualClassLoader] != null)
        this
    else
        with {
            ScriptEvaluationConfiguration.jvm.actualClassLoader(classLoader)
            if (this[ScriptEvaluationConfiguration.scriptsInstancesSharing] == true) {
                ScriptEvaluationConfiguration.jvm.scriptsInstancesSharingMap(mutableMapOf())
            }
        }
