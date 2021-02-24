/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarInputStream
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.impl.createScriptFromClassLoader
import kotlin.script.experimental.jvm.jvm

open class CompiledScriptJarsCache(val scriptToFile: (SourceCode, ScriptCompilationConfiguration) -> File?) :
    CompiledJvmScriptsCache {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? {
        val file = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")

        if (!file.exists()) return null

        val className = file.inputStream().use { ostr ->
            JarInputStream(ostr).use {
                it.manifest.mainAttributes.getValue("Main-Class")
            }
        }
        return KJvmCompiledScriptLazilyLoadedFromClasspath(className, listOf(file))
    }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val file = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")

        val jvmScript = (compiledScript as? KJvmCompiledScript)
            ?: throw IllegalArgumentException("Unsupported script type ${compiledScript::class.java.name}")

        jvmScript.saveToJar(file)
    }
}

private class KJvmCompiledScriptLazilyLoadedFromClasspath(
    private val scriptClassFQName: String,
    private val classPath: List<File>
) : CompiledScript {

    private var loadedScript: KJvmCompiledScript? = null

    fun getScriptOrError(): KJvmCompiledScript = loadedScript ?: throw RuntimeException("Compiled script is not loaded yet")

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> {
        if (loadedScript == null) {
            val actualEvaluationConfiguration = scriptEvaluationConfiguration ?: ScriptEvaluationConfiguration()
            val baseClassLoader = actualEvaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
            val classLoader = URLClassLoader(
                classPath.map { it.toURI().toURL() }.toTypedArray(),
                baseClassLoader
            )
            loadedScript = createScriptFromClassLoader(scriptClassFQName, classLoader)
        }
        return getScriptOrError().getClass(scriptEvaluationConfiguration)
    }

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = getScriptOrError().compilationConfiguration

    override val sourceLocationId: String?
        get() = getScriptOrError().sourceLocationId

    override val otherScripts: List<CompiledScript>
        get() = getScriptOrError().otherScripts

    override val resultField: Pair<String, KotlinType>?
        get() = getScriptOrError().resultField
}

