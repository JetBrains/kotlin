/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvmhost.impl.KJvmCompiledScript
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptEvaluationConfigurationKeys

open class JvmScriptEvaluationConfigurationBuilder : PropertiesCollection.Builder(), JvmScriptEvaluationConfigurationKeys {

    companion object : JvmScriptEvaluationConfigurationBuilder()
}

/**
 * The base classloader to use for script classes loading
 */
val JvmScriptEvaluationConfigurationKeys.baseClassLoader by PropertiesCollection.key<ClassLoader?>(Thread.currentThread().contextClassLoader)

/**
 * Load script dependencies before evaluation, true by default
 * If false, it is assumed that the all dependencies will be provided via baseClassLoader
 */
val JvmScriptEvaluationConfigurationKeys.loadDependencies by PropertiesCollection.key<Boolean>(true)

internal val JvmScriptEvaluationConfigurationKeys.actualClassLoader by PropertiesCollection.key<ClassLoader?>()

internal val JvmScriptEvaluationConfigurationKeys.scriptsInstancesSharingMap by PropertiesCollection.key<MutableMap<KClass<*>, EvaluationResult>>()

val ScriptEvaluationConfigurationKeys.jvm get() = JvmScriptEvaluationConfigurationBuilder()

class JvmScriptEvaluationContext(
    val classLoader: ClassLoader,
    val scriptsInstancesSharingMap: MutableMap<KClass<*>, EvaluationResult>?
)

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
                            ?.handler?.invoke(ScriptEvaluationConfigurationRefinementContext(compiledScript, configuration))
                            ?.onFailure {
                                return@invoke ResultWithDiagnostics.Failure(it.reports)
                            }
                            ?.resultOrNull()
                            ?: configuration

                    scriptClass.evalWithConfigAndOtherScriptsResults(refinedEvalConfiguration, importedScriptsEvalResults).let {
                        sharedScripts?.put(scriptClass, it)
                        ResultWithDiagnostics.Success(it)
                    }
                }
        }
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(e.asDiagnostics("Error evaluating script", path = compiledScript.sourceLocationId))
    }

    private fun getConfigurationWithClassloader(
        script: CompiledScript<*>, baseConfiguration: ScriptEvaluationConfiguration
    ): ScriptEvaluationConfiguration =
        if (baseConfiguration.containsKey(ScriptEvaluationConfiguration.jvm.actualClassLoader))
            baseConfiguration
        else {
            val jvmScript = (script as? KJvmCompiledScript<*>)
                ?: throw IllegalArgumentException("Unexpected compiled script type: $script")

            val classloader = jvmScript.getOrCreateActualClassloader(baseConfiguration)

            ScriptEvaluationConfiguration(baseConfiguration) {
                ScriptEvaluationConfiguration.jvm.actualClassLoader(classloader)
                if (baseConfiguration[ScriptEvaluationConfiguration.scriptsInstancesSharing] == true) {
                    ScriptEvaluationConfiguration.jvm.scriptsInstancesSharingMap(mutableMapOf())
                }
            }
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

internal fun KJvmCompiledScript<*>.getOrCreateActualClassloader(evaluationConfiguration: ScriptEvaluationConfiguration): ClassLoader =
    evaluationConfiguration[ScriptEvaluationConfiguration.jvm.actualClassLoader] ?: run {
        val baseClassLoader = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
        val classLoaderWithDeps =
            if (evaluationConfiguration[ScriptEvaluationConfiguration.jvm.loadDependencies] == false) baseClassLoader
            else makeClassLoaderFromDependencies(baseClassLoader)
        compiledModule.createClassLoader(classLoaderWithDeps)
    }

private fun CompiledScript<*>.makeClassLoaderFromDependencies(baseClassLoader: ClassLoader?): ClassLoader? {
    val processedScripts = mutableSetOf<CompiledScript<*>>()
    fun seq(res: Sequence<CompiledScript<*>>, script: CompiledScript<*>): Sequence<CompiledScript<*>> {
        if (processedScripts.contains(script)) return res
        processedScripts.add(script)
        return script.otherScripts.asSequence().fold(res + script, ::seq)
    }

    val dependencies = seq(emptySequence(), this).flatMap { script ->
        script.compilationConfiguration[ScriptCompilationConfiguration.dependencies]
            ?.asSequence()
            ?.flatMap { dep ->
                (dep as? JvmDependency)?.classpath?.asSequence()?.map { it.toURI().toURL() } ?: emptySequence()
            }
            ?: emptySequence()
    }.distinct()
    // TODO: previous dependencies and classloaders should be taken into account here
    return if (dependencies.none()) baseClassLoader
    else URLClassLoader(dependencies.toList().toTypedArray(), baseClassLoader)
}
