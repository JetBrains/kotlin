/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.basic

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.script.experimental.api.*


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptFileExtension(val extension: String)

open class DefaultScriptSelector(val baseClass: KClass<Any>? = null) : ScriptSelector {

    override val name: String = "Kotlin script"

    override val fileExtension: String = baseClass?.findAnnotation<KotlinScriptFileExtension>()?.extension ?: "kts"

    override fun makeScriptName(scriptFileName: String?): String = scriptFileName?.removeSuffix(".$fileExtension") ?: "Kotlin script"

    override fun isKnownScript(script: ScriptSource): Boolean =
        script.location?.file?.endsWith(fileExtension) ?: true
}

class PassThroughConfigurator(val baseClass: KClass<Any>? = null) : ScriptConfigurator {

    override suspend fun baseConfiguration(scriptSource: ScriptSource?): ResultWithDiagnostics<ScriptCompileConfiguration> =
        (when (scriptSource) {
            null -> ScriptCompileConfiguration()
            else -> ScriptCompileConfiguration(scriptSource.toConfigEntry())
        }).asSuccess()

    override suspend fun refineConfiguration(
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData
    ): ResultWithDiagnostics<ScriptCompileConfiguration> =
        configuration.asSuccess()
}

class DummyRunner<ScriptBase : Any>(val baseClass: KClass<ScriptBase>? = null) : ScriptRunner<ScriptBase> {
    override suspend fun run(
        compiledScript: CompiledScript<ScriptBase>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult> =
        ResultWithDiagnostics.Failure("not implemented".asErrorDiagnostics())
}

// TODO: from org.jetbrains.kotlin.utils.addToStdlib, take it from the stdlib when available
private inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

