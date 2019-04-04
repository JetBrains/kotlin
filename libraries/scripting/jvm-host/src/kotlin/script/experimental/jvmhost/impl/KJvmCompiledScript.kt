/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.getOrCreateActualClassloader

class KJvmCompiledScript<out ScriptBase : Any>(
    sourceLocationId: String?,
    compilationConfiguration: ScriptCompilationConfiguration,
    private var scriptClassFQName: String,
    otherScripts: List<CompiledScript<*>> = emptyList(),
    internal var compiledModule: KJvmCompiledModule
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
        compiledModule = inputStream.readObject() as KJvmCompiledModuleInMemory
        scriptClassFQName = inputStream.readObject() as String
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 2L
    }
}