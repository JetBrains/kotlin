/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import org.jetbrains.kotlin.codegen.state.GenerationState
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvmhost.JvmScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.baseClassLoader

class KJvmCompiledScript<out ScriptBase : Any>(
    compilationConfiguration: ScriptCompilationConfiguration,
    generationState: GenerationState,
    private var scriptClassFQName: String
) : CompiledScript<ScriptBase>, Serializable {

    private var _compilationConfiguration: ScriptCompilationConfiguration? = compilationConfiguration
    private var compilerOutputFiles: Map<String, ByteArray> = run {
        val res = sortedMapOf<String, ByteArray>()
        for (it in generationState.factory.asList()) {
            res[it.relativePath] = it.asByteArray()
        }
        res
    }

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = _compilationConfiguration!!

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> = try {
        val baseClassLoader = scriptEvaluationConfiguration?.get(JvmScriptEvaluationConfiguration.baseClassLoader)
            ?: Thread.currentThread().contextClassLoader
        val dependencies = compilationConfiguration[ScriptCompilationConfiguration.dependencies]
            ?.flatMap { (it as? JvmDependency)?.classpath?.map { it.toURI().toURL() } ?: emptyList() }
        // TODO: previous dependencies and classloaders should be taken into account here
        val classLoaderWithDeps =
            if (dependencies == null) baseClassLoader
            else URLClassLoader(dependencies.toTypedArray(), baseClassLoader)
        val classLoader = CompiledScriptClassLoader(classLoaderWithDeps, compilerOutputFiles)

        val clazz = classLoader.loadClass(scriptClassFQName).kotlin
        clazz.asSuccess()
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                "Unable to instantiate class $scriptClassFQName",
                exception = e
            )
        )
    }

    // This method is exposed because the compilation configuration is not generally serializable (yet), but since it is supposed to
    // be deserialized only from the cache, the configuration could be assigned from the cache.load method
    fun setCompilationConfiguration(configuration: ScriptCompilationConfiguration) {
        if (_compilationConfiguration != null) throw IllegalStateException("This method is applicable only in deserialization context")
        _compilationConfiguration = configuration
    }

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(compilerOutputFiles)
        outputStream.writeObject(scriptClassFQName)
    }

    private fun readObject(inputStream: ObjectInputStream) {
        _compilationConfiguration = null
        compilerOutputFiles = inputStream.readObject() as Map<String, ByteArray>
        scriptClassFQName = inputStream.readObject() as String
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}