/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.*
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.actualClassLoader

internal class KJvmCompiledScriptData(
    var sourceLocationId: String?,
    var compilationConfiguration: ScriptCompilationConfiguration,
    var scriptClassFQName: String,
    var resultField: Pair<String, KotlinType>?,
    var otherScripts: List<CompiledScript<*>> = emptyList()
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
        otherScripts = inputStream.readObject() as List<CompiledScript<*>>
        scriptClassFQName = inputStream.readObject() as String
        resultField = inputStream.readObject() as Pair<String, KotlinType>?
    }

    companion object {
        @JvmStatic
        private val serialVersionUID = 4L
    }
}

class KJvmCompiledScript<out ScriptBase : Any> internal constructor(
    internal var data: KJvmCompiledScriptData,
    var compiledModule: KJvmCompiledModule? // module should be null for imported (other) scripts, so only one reference to the module is kept
) : CompiledScript<ScriptBase>, Serializable {

    constructor(
        sourceLocationId: String?,
        compilationConfiguration: ScriptCompilationConfiguration,
        scriptClassFQName: String,
        resultField: Pair<String, KotlinType>?,
        otherScripts: List<CompiledScript<*>> = emptyList(),
        compiledModule: KJvmCompiledModule? // module should be null for imported (other) scripts, so only one reference to the module is kept
    ) : this(
        KJvmCompiledScriptData(sourceLocationId, compilationConfiguration, scriptClassFQName, resultField, otherScripts),
        compiledModule
    )

    override val sourceLocationId: String?
        get() = data.sourceLocationId

    override val compilationConfiguration: ScriptCompilationConfiguration
        get() = data.compilationConfiguration

    override val otherScripts: List<CompiledScript<*>>
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
                "Unable to instantiate class ${data.scriptClassFQName}",
                sourcePath = sourceLocationId,
                exception = e
            )
        )
    }

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

fun KJvmCompiledScript<*>.getOrCreateActualClassloader(evaluationConfiguration: ScriptEvaluationConfiguration): ClassLoader =
    evaluationConfiguration[ScriptEvaluationConfiguration.jvm.actualClassLoader] ?: run {
        val module = compiledModule
            ?: throw IllegalStateException("Illegal call sequence, actualClassloader should be set before calling function on the class without module")
        val baseClassLoader = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]
        val classLoaderWithDeps =
            if (evaluationConfiguration[ScriptEvaluationConfiguration.jvm.loadDependencies] == false) baseClassLoader
            else makeClassLoaderFromDependencies(baseClassLoader)
        return module.createClassLoader(classLoaderWithDeps)
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

const val KOTLIN_SCRIPT_METADATA_PATH = "META-INF/kotlin/script"
const val KOTLIN_SCRIPT_METADATA_EXTENSION_WITH_DOT = ".kotlin_script"
fun scriptMetadataPath(scriptClassFQName: String) =
    "$KOTLIN_SCRIPT_METADATA_PATH/$scriptClassFQName$KOTLIN_SCRIPT_METADATA_EXTENSION_WITH_DOT"

fun <T : Any> KJvmCompiledScript<T>.copyWithoutModule(): KJvmCompiledScript<T> = KJvmCompiledScript(data, null)

fun KJvmCompiledScript<*>.toBytes(): ByteArray {
    val bos = ByteArrayOutputStream()
    var oos: ObjectOutputStream? = null
    try {
        oos = ObjectOutputStream(bos)
        oos.writeObject(this)
        oos.flush()
        return bos.toByteArray()!!
    } finally {
        try {
            oos?.close()
        } catch (e: IOException) {
        }
    }
}

fun createScriptFromClassLoader(scriptClassFQName: String, classLoader: ClassLoader): KJvmCompiledScript<*> {
    val scriptDataStream = classLoader.getResourceAsStream(scriptMetadataPath(scriptClassFQName))
        ?: throw IllegalArgumentException("Cannot find metadata for script $scriptClassFQName")
    val script = ObjectInputStream(scriptDataStream).use {
        it.readObject() as KJvmCompiledScript<*>
    }
    script.compiledModule = KJvmCompiledModuleFromClassLoader(classLoader)
    return script
}
