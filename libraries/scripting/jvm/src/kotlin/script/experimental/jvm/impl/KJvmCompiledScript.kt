/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.*
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.*

internal class KJvmCompiledScriptData(
    var sourceLocationId: String?,
    var compilationConfiguration: ScriptCompilationConfiguration,
    var scriptClassFQName: String,
    var resultField: Pair<String, KotlinType>?,
    var otherScripts: List<CompiledScript> = emptyList()
) : Serializable {

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(compilationConfiguration)
        outputStream.writeObject(sourceLocationId)
        outputStream.writeObject(otherScripts)
        outputStream.writeObject(scriptClassFQName)
        outputStream.writeObject(resultField)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(inputStream: ObjectInputStream) {
        compilationConfiguration = inputStream.readObject() as ScriptCompilationConfiguration
        sourceLocationId = inputStream.readObject() as String?
        otherScripts = inputStream.readObject() as List<CompiledScript>
        scriptClassFQName = inputStream.readObject() as String
        resultField = inputStream.readObject() as Pair<String, KotlinType>?
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 5L
    }
}

open class KJvmCompiledScript internal constructor(
    internal var data: KJvmCompiledScriptData,
    internal var compiledModule: KJvmCompiledModule? // module should be null for imported (other) scripts, so only one reference to the module is kept
) : CompiledScript, Serializable {

    constructor(
        sourceLocationId: String?,
        compilationConfiguration: ScriptCompilationConfiguration,
        scriptClassFQName: String,
        resultField: Pair<String, KotlinType>?,
        otherScripts: List<CompiledScript> = emptyList(),
        compiledModule: KJvmCompiledModule? // module should be null for imported (other) scripts, so only one reference to the module is kept
    ) : this(
        KJvmCompiledScriptData(sourceLocationId, compilationConfiguration, scriptClassFQName, resultField, otherScripts),
        compiledModule
    )

    override val sourceLocationId: String?
        get() = data.sourceLocationId

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = data.compilationConfiguration

    override val otherScripts: List<CompiledScript>
        get() = data.otherScripts

    val scriptClassFQName: String
        get() = data.scriptClassFQName

    override val resultField: Pair<String, KotlinType>?
        get() = data.resultField

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> = try {
        // ensuring proper defaults are used
        val actualEvaluationConfiguration = scriptEvaluationConfiguration ?: ScriptEvaluationConfiguration()
        val classLoader = getOrCreateActualClassloader(actualEvaluationConfiguration)

        val clazz = classLoader.loadClass(data.scriptClassFQName).kotlin
        clazz.asSuccess()
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                "Unable to instantiate class ${data.scriptClassFQName}",
                sourcePath = sourceLocationId,
                exception = e
            )
        )
    }

    fun getCompiledModule() = compiledModule

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(data)
        outputStream.writeObject(compiledModule)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(inputStream: ObjectInputStream) {
        data = inputStream.readObject() as KJvmCompiledScriptData
        compiledModule = inputStream.readObject() as KJvmCompiledModule?
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 3L
    }
}

fun KJvmCompiledScript.getOrCreateActualClassloader(evaluationConfiguration: ScriptEvaluationConfiguration): ClassLoader =
    evaluationConfiguration[ScriptEvaluationConfiguration.jvm.actualClassLoader] ?: run {
        val module = compiledModule
            ?: throw IllegalStateException("Illegal call sequence, actualClassloader should be set before calling function on the class without module")
        val baseClassLoader = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
        val lastClassLoader = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.lastSnippetClassLoader] ?: baseClassLoader
        val classLoaderWithDeps =
            if (evaluationConfiguration[ScriptEvaluationConfiguration.jvm.loadDependencies] == false) baseClassLoader
            else makeClassLoaderFromDependencies(baseClassLoader, lastClassLoader)
        return module.createClassLoader(classLoaderWithDeps)
    }

private fun CompiledScript.makeClassLoaderFromDependencies(baseClassLoader: ClassLoader?, lastClassLoader: ClassLoader?): ClassLoader? {
    val processedScripts = mutableSetOf<CompiledScript>()
    fun recursiveScriptsSeq(res: Sequence<CompiledScript>, script: CompiledScript): Sequence<CompiledScript> =
        if (processedScripts.add(script)) script.otherScripts.asSequence().fold(res + script, ::recursiveScriptsSeq)
        else res

    val dependenciesWithConfigurations = recursiveScriptsSeq(emptySequence(), this).flatMap { script ->
        script.compilationConfiguration[ScriptCompilationConfiguration.dependencies]
            ?.asSequence()?.map { script.compilationConfiguration to it } ?: emptySequence()
    }

    val processedClasspathElements = mutableSetOf<URL>()
    fun recursiveClassPath(res: Sequence<URL>, classLoader: ClassLoader?): Sequence<URL> =
        when (classLoader) {
            null, baseClassLoader -> res
            is DualClassLoader -> recursiveClassPath(res, classLoader.parent) +
                    recursiveClassPath(emptySequence(), classLoader.fallbackClassLoader)
            is URLClassLoader -> recursiveClassPath(res + classLoader.urLs, classLoader.parent)
            else -> recursiveClassPath(res, classLoader.parent)
        }
    recursiveClassPath(emptySequence(), lastClassLoader).forEach { processedClasspathElements.add(it) }

    val processedClassloaders = mutableSetOf<ClassLoader>()

    return dependenciesWithConfigurations.fold(lastClassLoader) { parentClassLoader, (compilationConfiguration, scriptDependency) ->
        when (scriptDependency) {
            is JvmDependency -> {
                scriptDependency.classpath.mapNotNull {
                    val url = it.toURI().toURL()
                    if (processedClasspathElements.add(url)) url else null
                }.takeUnless { it.isEmpty() }?.let { URLClassLoader(it.toTypedArray(), parentClassLoader) }
            }
            is JvmDependencyFromClassLoader -> {
                val dependenciesClassLoader = scriptDependency.getClassLoader(compilationConfiguration)
                if (processedClassloaders.add(dependenciesClassLoader)) DualClassLoader(dependenciesClassLoader, parentClassLoader)
                else null
            }
            else -> null
        } ?: parentClassLoader
    }
}

const val KOTLIN_SCRIPT_METADATA_PATH = "META-INF/kotlin/script"
const val KOTLIN_SCRIPT_METADATA_EXTENSION_WITH_DOT = ".kotlin_script"
fun scriptMetadataPath(scriptClassFQName: String) =
    "$KOTLIN_SCRIPT_METADATA_PATH/$scriptClassFQName$KOTLIN_SCRIPT_METADATA_EXTENSION_WITH_DOT"

fun KJvmCompiledScript.copyWithoutModule(): KJvmCompiledScript = KJvmCompiledScript(data, null)

fun KJvmCompiledScript.toBytes(): ByteArray {
    val bos = ByteArrayOutputStream()
    var oos: ObjectOutputStream? = null
    try {
        oos = ObjectOutputStream(bos)
        oos.writeObject(this)
        oos.flush()
        return bos.toByteArray()
    } finally {
        try {
            oos?.close()
        } catch (e: IOException) {
        }
    }
}

fun createScriptFromClassLoader(scriptClassFQName: String, classLoader: ClassLoader): KJvmCompiledScript {
    val scriptDataStream = classLoader.getResourceAsStream(scriptMetadataPath(scriptClassFQName))
        ?: throw IllegalArgumentException("Cannot find metadata for script $scriptClassFQName")
    val script = ObjectInputStream(scriptDataStream).use {
        it.readObject() as KJvmCompiledScript
    }
    script.compiledModule = KJvmCompiledModuleFromClassLoader(classLoader)
    return script
}
