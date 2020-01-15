/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.mainKts.impl.IvyResolver
import org.jetbrains.kotlin.mainKts.impl.resolveFromAnnotations
import org.jetbrains.kotlin.script.util.CompilerOptions
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Import
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import java.security.MessageDigest
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223

@Suppress("unused")
@KotlinScript(
    fileExtension = "main.kts",
    compilationConfiguration = MainKtsScriptDefinition::class,
    evaluationConfiguration = MainKtsEvaluationConfiguration::class
)
abstract class MainKtsScript(val args: Array<String>)

const val COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR = "KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR"
const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "kotlin.main.kts.compiled.scripts.cache.dir"

class MainKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        jvm {
            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kotlin-main-kts", "kotlin-stdlib", "kotlin-reflect")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, handler = MainKtsConfigurator())
            beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        jsr223 {
            importAllBindings(true)
        }
        hostConfiguration(ScriptingHostConfiguration {
            jvm {
                val cacheExtSetting = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
                    ?: System.getenv(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR)
                val cacheBaseDir = when {
                    cacheExtSetting == null -> System.getProperty("java.io.tmpdir")
                        ?.let(::File)?.takeIf { it.exists() && it.isDirectory }
                        ?.let { File(it, "main.kts.compiled.cache").apply { mkdir() } }
                    cacheExtSetting.isBlank() -> null
                    else -> File(cacheExtSetting)
                }?.takeIf { it.exists() && it.isDirectory }
                if (cacheBaseDir != null)
                    compilationCache(
                        CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                            File(cacheBaseDir, compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar")
                        }
                    )
            }
        })
    })

object MainKtsEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
        refineConfigurationBeforeEvaluate(::configureConstructorArgsFromMainArgs)
    }
)

fun configureConstructorArgsFromMainArgs(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val mainArgs = context.evaluationConfiguration[ScriptEvaluationConfiguration.jvm.mainArguments]
    val res = if (context.evaluationConfiguration[ScriptEvaluationConfiguration.constructorArgs] == null && mainArgs != null) {
        context.evaluationConfiguration.with {
            constructorArgs(mainArgs)
        }
    } else context.evaluationConfiguration
    return res.asSuccess()
}

class MainKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), IvyResolver())

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        processAnnotations(context)

    fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                ScriptDiagnostic(
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = annotations.flatMap {
            (it as? Import)?.paths?.map { sourceName ->
                FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
            } ?: emptyList()
        }
        val compileOptions = annotations.flatMap {
            (it as? CompilerOptions)?.options?.toList() ?: emptyList()
        }

        val resolveResult = try {
            runBlocking {
                resolveFromAnnotations(resolver, annotations.filter { it is DependsOn || it is Repository })
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics(path = context.script.locationId))
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                if (resolvedClassPath != null) updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources)
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}

private fun compiledScriptUniqueName(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
