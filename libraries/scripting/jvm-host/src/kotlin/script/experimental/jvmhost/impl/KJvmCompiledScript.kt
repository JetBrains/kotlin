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
import kotlin.script.experimental.jvmhost.*

class KJvmCompiledModule(
    generationState: GenerationState
) : Serializable {
    val compilerOutputFiles: Map<String, ByteArray> =
        generationState.factory.asList()
            .associateTo(sortedMapOf<String, ByteArray>()) { it.relativePath to it.asByteArray() }

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}

class KJvmCompiledScript<out ScriptBase : Any>(
    sourceLocationId: String?,
    compilationConfiguration: ScriptCompilationConfiguration,
    private var scriptClassFQName: String,
    otherScripts: List<CompiledScript<*>> = emptyList(),
    private var compiledModule: KJvmCompiledModule? = null
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
        val classLoader = actualEvaluationConfiguration[ScriptEvaluationConfiguration.jvm.actualClassLoader]
            ?: run {
                if (compiledModule == null)
                    return ResultWithDiagnostics.Failure(
                        "Unable to load class $scriptClassFQName: no compiled module is provided".asErrorDiagnostics(path = sourceLocationId)
                    )
                val baseClassLoader = actualEvaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
                val dependencies = compilationConfiguration[ScriptCompilationConfiguration.dependencies]
                    ?.flatMap { (it as? JvmDependency)?.classpath?.map { it.toURI().toURL() } ?: emptyList() }
                // TODO: previous dependencies and classloaders should be taken into account here
                val classLoaderWithDeps =
                    if (dependencies == null) baseClassLoader
                    else URLClassLoader(dependencies.toTypedArray(), baseClassLoader)
                CompiledScriptClassLoader(classLoaderWithDeps, compiledModule!!.compilerOutputFiles)
            }

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

    // This method is exposed because the compilation configuration is not generally serializable (yet), but since it is supposed to
    // be deserialized only from the cache, the configuration could be assigned from the cache.load method
    fun setCompilationConfiguration(configuration: ScriptCompilationConfiguration) {
        if (_compilationConfiguration != null) throw IllegalStateException("This method is applicable only in deserialization context")
        _compilationConfiguration = configuration
    }

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(sourceLocationId)
        outputStream.writeObject(otherScripts)
        outputStream.writeObject(compiledModule)
        outputStream.writeObject(scriptClassFQName)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(inputStream: ObjectInputStream) {
        _compilationConfiguration = null
        _sourceLocationId = inputStream.readObject() as String?
        _otherScripts = inputStream.readObject() as List<CompiledScript<*>>
        compiledModule = inputStream.readObject() as KJvmCompiledModule?
        scriptClassFQName = inputStream.readObject() as String
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 1L
    }
}