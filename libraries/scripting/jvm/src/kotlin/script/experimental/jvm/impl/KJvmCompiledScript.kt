/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.actualClassLoader

class KJvmCompiledScript<out ScriptBase : Any>(
    sourceLocationId: String?,
    compilationConfiguration: ScriptCompilationConfiguration,
    private var scriptClassFQName: String,
    otherScripts: List<CompiledScript<*>> = emptyList(),
    var compiledModule: KJvmCompiledModule
) : CompiledScript<ScriptBase>, Serializable {

    private var _sourceLocationId: String? = sourceLocationId

    override val sourceLocationId: String?
        get() = _sourceLocationId

    private var _compilationConfiguration: ScriptCompilationConfiguration? = compilationConfiguration

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = _compilationConfiguration!!

    private var _otherScripts: List<CompiledScript<*>> = otherScripts

    override val otherScripts: List<CompiledScript<*>>
        get() = _otherScripts

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> = try {
        // ensuring proper defaults are used
        val actualEvaluationConfiguration = scriptEvaluationConfiguration ?: ScriptEvaluationConfiguration()
        val classLoader = getOrCreateActualClassloader(actualEvaluationConfiguration)

        val clazz = classLoader.loadClass(scriptClassFQName).kotlin
        clazz.asSuccess()
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                "Unable to instantiate class $scriptClassFQName",
                sourcePath = sourceLocationId,
                exception = e
            )
        )
    }

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(compilationConfiguration)
        outputStream.writeObject(sourceLocationId)
        outputStream.writeObject(otherScripts)
        outputStream.writeObject(compiledModule)
        outputStream.writeObject(scriptClassFQName)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(inputStream: ObjectInputStream) {
        _compilationConfiguration = inputStream.readObject() as ScriptCompilationConfiguration
        _sourceLocationId = inputStream.readObject() as String?
        _otherScripts = inputStream.readObject() as List<CompiledScript<*>>
        compiledModule = inputStream.readObject() as KJvmCompiledModule
        scriptClassFQName = inputStream.readObject() as String
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 2L
    }
}

fun KJvmCompiledScript<*>.getOrCreateActualClassloader(evaluationConfiguration: ScriptEvaluationConfiguration): ClassLoader =
    evaluationConfiguration[ScriptEvaluationConfiguration.jvm.actualClassLoader] ?: run {
        val baseClassLoader = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
        val classLoaderWithDeps =
            if (evaluationConfiguration[ScriptEvaluationConfiguration.jvm.loadDependencies] == false) baseClassLoader
            else makeClassLoaderFromDependencies(baseClassLoader)
        compiledModule.createClassLoader(classLoaderWithDeps)
    }

fun getConfigurationWithClassloader(
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

val ScriptEvaluationConfiguration.sharedScripts get() = get(ScriptEvaluationConfiguration.jvm.scriptsInstancesSharingMap)

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
