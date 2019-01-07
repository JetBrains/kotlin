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

val JvmScriptEvaluationConfiguration.actualClassLoader by PropertiesCollection.key<ClassLoader?>()

val ScriptEvaluationConfiguration.jvm get() = JvmScriptEvaluationConfiguration()

open class BasicJvmScriptEvaluator : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> =
        try {
            compiledScript.getClass(scriptEvaluationConfiguration).onSuccess { scriptClass ->
                // in the future, when (if) we'll stop to compile everything into constructor
                // run as SAM
                // return res

                // for other scripts we need evaluation configuration with actualClassloader set,
                // so they are loaded in the same classloader as the "main" script
                val updatedEvalConfiguration = when {
                    scriptEvaluationConfiguration == null -> ScriptEvaluationConfiguration {
                        // TODO: find out why dsl syntax doesn't work here
                        set(JvmScriptEvaluationConfiguration.actualClassLoader, scriptClass.java.classLoader)
                    }
                    scriptEvaluationConfiguration.getNoDefault(JvmScriptEvaluationConfiguration.actualClassLoader) == null ->
                        ScriptEvaluationConfiguration(scriptEvaluationConfiguration) {
                            // TODO: find out why dsl syntax doesn't work here
                            set(JvmScriptEvaluationConfiguration.actualClassLoader, scriptClass.java.classLoader)
                        }
                    else -> scriptEvaluationConfiguration
                }

                val sharedScripts = scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.scriptsInstancesSharingMap)

                val instanceFromShared = sharedScripts?.get(scriptClass)

                if (instanceFromShared != null) {
                    instanceFromShared.asSuccess(updatedEvalConfiguration)
                } else {

                    val args = ArrayList<Any?>()

                    updatedEvalConfiguration[ScriptEvaluationConfiguration.constructorArgs]?.let {
                        args.addAll(it)
                    }
                    scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.providedProperties)?.forEach {
                        args.add(it.value)
                    }
                    scriptEvaluationConfiguration?.get(ScriptEvaluationConfiguration.implicitReceivers)?.let {
                        args.addAll(it)
                    }

                    compiledScript.otherScripts.mapSuccess {
                        invoke(it, updatedEvalConfiguration)
                    }.onSuccess { importedScriptsEvalResults ->

                        importedScriptsEvalResults.forEach {
                            args.add((it.returnValue as ResultValue.Value).scriptInstance)
                        }

                        val ctor = scriptClass.java.constructors.single()
                        val instance = ctor.newInstance(*args.toArray())

                        sharedScripts?.put(scriptClass, instance)

                        instance.asSuccess(updatedEvalConfiguration)
                    }
                }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics("Error evaluating script", path = compiledScript.sourceLocationId))
        }
}

private fun Any.asSuccess(updatedEvalConfiguration: ScriptEvaluationConfiguration) =
// TODO: fix result value when ready
    ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Value("", this, "", this), updatedEvalConfiguration))
