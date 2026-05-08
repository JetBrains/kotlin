/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts

import org.jetbrains.kotlin.mainKts.impl.Directories
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223
import kotlin.script.experimental.jvmhost.loadScriptFromJar
import kotlin.script.experimental.jvmhost.saveToJar
import kotlin.script.experimental.util.filterByAnnotationType

@Suppress("unused")
@KotlinScript(
    fileExtension = "main.kts",
    compilationConfiguration = MainKtsScriptDefinition::class,
    evaluationConfiguration = MainKtsEvaluationConfiguration::class,
    hostConfiguration = MainKtsHostConfiguration::class
)
abstract class MainKtsScript(val args: Array<String>)

const val COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR = "KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR"
const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "kotlin.main.kts.compiled.scripts.cache.dir"
const val COMPILED_SCRIPTS_CACHE_VERSION = 2
const val SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME = "__FILE__"

class MainKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, ScriptFileLocation::class)
        jvm {
            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kotlin-main-kts", "kotlin-stdlib", "kotlin-reflect")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, handler = MainKtsConfigurator())
            onAnnotations(ScriptFileLocation::class, handler = ScriptFileLocationCustomConfigurator())
            beforeCompiling(::configureScriptFileLocationPathVariablesForCompilation)
            beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        jsr223 {
            importAllBindings(true)
        }
    }
)

object MainKtsEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        refineConfigurationBeforeEvaluate(::configureScriptFileLocationPathVariablesForEvaluation)
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
        refineConfigurationBeforeEvaluate(::configureConstructorArgsFromMainArgs)
    }
)

class MainKtsHostConfiguration : ScriptingHostConfiguration(
    {
        jvm {
            val cacheExtSetting = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
                ?: System.getenv(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR)
            val cacheBaseDir = when {
                cacheExtSetting == null -> Directories(System.getProperties(), System.getenv()).cache
                    ?.takeIf { it.exists() && it.isDirectory }
                    ?.let { File(it, "main.kts.compiled.cache").apply { mkdir() } }
                cacheExtSetting.isBlank() -> null
                else -> File(cacheExtSetting)
            }?.takeIf { it.exists() && it.isDirectory }
            if (cacheBaseDir != null)
                compilationCache(MainKtsCompiledScriptJarsCache(cacheBaseDir))
        }
    }
)

fun configureScriptFileLocationPathVariablesForEvaluation(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val compilationConfiguration = context.evaluationConfiguration[ScriptEvaluationConfiguration.compilationConfiguration]
        ?: throw RuntimeException()
    val scriptFileLocation = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocation]
        ?: return context.evaluationConfiguration.asSuccess()
    val scriptFileLocationVariable = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
        ?: return context.evaluationConfiguration.asSuccess()

    val res = context.evaluationConfiguration.with {
        providedProperties.put(mapOf(scriptFileLocationVariable to scriptFileLocation))
    }
    return res.asSuccess()
}

fun configureScriptFileLocationPathVariablesForCompilation(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val scriptFile = context.script.locationId?.let(::File) ?: return context.compilationConfiguration.asSuccess()
    val scriptFileLocationVariableName = context.compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
        ?: SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        providedProperties.put(mapOf(scriptFileLocationVariableName to KotlinType(File::class)))
        scriptFileLocation.put(scriptFile)
        scriptFileLocationVariable.put(scriptFileLocationVariableName)
    }.asSuccess()
}

class ScriptFileLocationCustomConfigurator : RefineScriptCompilationConfigurationHandler {

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {

        val scriptLocationVariable = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
            ?.filterByAnnotationType<ScriptFileLocation>()?.firstOrNull()?.annotation?.variable
            ?: return context.compilationConfiguration.asSuccess()

        val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
            scriptFileLocationVariable.put(scriptLocationVariable)
        }

        return compilationConfiguration.asSuccess()
    }
}

fun configureConstructorArgsFromMainArgs(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val mainArgs = context.evaluationConfiguration[ScriptEvaluationConfiguration.jvm.mainArguments]
    val res = if (context.evaluationConfiguration[ScriptEvaluationConfiguration.constructorArgs] == null && mainArgs != null) {
        context.evaluationConfiguration.with {
            constructorArgs(mainArgs)
        }
    } else context.evaluationConfiguration
    return res.asSuccess()
}

class MainKtsConfigurator(
    private val resolver: ExternalDependenciesResolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver()),
) : RefineScriptCompilationConfigurationHandler, ConfiguratorWithDependencyResolver<MainKtsConfigurator> {

    override fun transformResolver(transform: (ExternalDependenciesResolver) -> ExternalDependenciesResolver) =
        MainKtsConfigurator(transform(resolver))

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        processAnnotations(context)

    fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = linkedMapOf<String, Pair<File, String>>()
        var hasImportErrors = false
        annotations.filterByAnnotationType<Import>().forEach { scriptAnnotation ->
            scriptAnnotation.annotation.paths.forEach { sourceName ->
                val file = (scriptBaseDir?.resolve(sourceName) ?: File(sourceName)).normalize()
                val keyPath = file.absolutePath
                val prevImport = importedSources.put(keyPath, file to sourceName)
                if (prevImport != null) {
                    diagnostics.add(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError, "Duplicate imports: \"${prevImport.second}\" and \"$sourceName\"",
                            sourcePath = context.script.locationId, location = scriptAnnotation.location?.locationInText
                        )
                    )
                    hasImportErrors = true
                }
            }
        }
        if (hasImportErrors) return ResultWithDiagnostics.Failure(diagnostics)

        val compileOptions = annotations.filterByAnnotationType<CompilerOptions>().flatMap {
            it.annotation.options.toList()
        }

        val resolveResult = try {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                resolver.resolveFromScriptSourceAnnotations(
                    annotations.filter {
                        when (it.annotation) {
                            is DependsOn,
                            is Repository -> true
                            else ->
                                if ((it.annotation::class.simpleName?.let { it == "DependsOn" || it == "Repository" }) == true )
                                    error("Annotation ${it.annotation::class.simpleName} loaded in another classloader")
                                else false
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            diagnostics.add(e.asDiagnostics(path = context.script.locationId))
            ResultWithDiagnostics.Failure(diagnostics)
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources.values.map { FileScriptSource(it.first) })
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}

private fun compiledScriptUniqueName(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("SHA-256")

    fun addToDigest(chunk: String) = with(digestWrapper) {
        val chunkBytes = chunk.toByteArray()
        update(chunkBytes.size.toByteArray())
        update(chunkBytes)
    }

    digestWrapper.update(COMPILED_SCRIPTS_CACHE_VERSION.toByteArray())
    addToDigest(script.text)
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            addToDigest(it.key.name)
            addToDigest(it.value.toString())
        }
    return digestWrapper.digest().toHexString()
}

private class MainKtsCompiledScriptJarsCache(private val cacheBaseDir: File) :
    CompiledScriptJarsCache({ script, compilationConfiguration ->
                                File(cacheBaseDir, compiledScriptUniqueName(script, compilationConfiguration) + ".jar")
                            }) {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? {
        val jar = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")
        if (!jar.exists()) return null
        val importsMetadata = File(jar.path + ".imports")
        if (!importsMetadata.exists() || !verifyMetadata(importsMetadata)) {
            jar.delete()
            importsMetadata.delete()
            return null
        }
        return jar.loadScriptFromJar() ?: run {
            jar.delete()
            importsMetadata.delete()
            null
        }
    }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ) {
        val jar = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")
        val jvmScript = (compiledScript as? KJvmCompiledScript)
            ?: throw IllegalArgumentException("Unsupported script type ${compiledScript::class.java.name}")
        jvmScript.saveToJar(jar)
        File(jar.path + ".imports").writeText(collectImportedFiles(jvmScript).joinToString("\n") { (path, hash) -> "$path\t$hash" })
    }

    private fun collectImportedFiles(script: CompiledScript): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        fun traverse(s: CompiledScript) {
            for (other in s.otherScripts) {
                val path = other.sourceLocationId ?: continue
                if (visited.add(path)) {
                    val file = File(path)
                    if (file.isFile) result.add(path to file.sha256Hex())
                    traverse(other)
                }
            }
        }
        traverse(script)
        return result
    }

    private fun verifyMetadata(file: File): Boolean =
        file.readLines().filter { it.isNotEmpty() }.all { line ->
            val parts = line.split("\t", limit = 2)
            if (parts.size != 2) return false
            val pathname = parts[0]
            val sha256 = parts[1]
            File(pathname).takeIf { it.isFile }?.sha256Hex() == sha256
        }

    private fun File.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buf = ByteArray(8192)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().toHexString()
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES).also { it.putInt(this) }.array()

