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
                    val importedScriptsReports = ArrayList<ScriptDiagnostic>()
                    var importedScriptsLoadingFailed = false

                    // for other scripts we need evaluation configuration with actualClassloader set,
                    // so they are loaded in the same classloader as the "main" script
                    val updatedEvalConfiguration = when {
                        scriptEvaluationConfiguration == null -> ScriptEvaluationConfiguration {
                            // TODO: find out why dsl syntax doesn't work here
                            set(JvmScriptEvaluationConfiguration.actualClassLoader, scriptClass.java.classLoader)
                        }
                        scriptEvaluationConfiguration.getNoDefault(JvmScriptEvaluationConfiguration.actualClassLoader) == null -> ScriptEvaluationConfiguration(scriptEvaluationConfiguration) {
                            // TODO: find out why dsl syntax doesn't work here
                            set(JvmScriptEvaluationConfiguration.actualClassLoader, scriptClass.java.classLoader)
                        }
                        else -> scriptEvaluationConfiguration
                    }

                    compiledScript.otherScripts.forEach {
                        // TODO: in the future other scripts could be used for other purposes, so args here should be added only for actually imported scripts
                        // (it means that we should keep mapping somewhere (or reuse one with source dependencies) between imported scrips and e.g. fqnames)
                        val importedScriptEvalRes = invoke(it, updatedEvalConfiguration)
                        importedScriptsReports.addAll(importedScriptEvalRes.reports)
                        when (importedScriptEvalRes) {
                            is ResultWithDiagnostics.Success -> {
                                // TODO: checks and diagnostics
                                args.add((importedScriptEvalRes.value.returnValue as ResultValue.Value).value)
                            }
                            else -> {
                                importedScriptsLoadingFailed = true
                                return@forEach
                            }
                        }
                    }
                    if (importedScriptsLoadingFailed) {
                        ResultWithDiagnostics.Failure(importedScriptsReports)
                    } else {

                        updatedEvalConfiguration[ScriptEvaluationConfiguration.constructorArgs]?.let {
                            args.addAll(it)
                        }
                        val ctor = scriptClass.java.constructors.single()
                        val instance = ctor.newInstance(*args.toArray())

                        // TODO: fix result value
                        ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Value("", instance, ""), updatedEvalConfiguration))
                    }
                }
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(e.asDiagnostics("Error evaluating script"))
        }
}
