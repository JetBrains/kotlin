/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmCompiledModuleInMemory
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.impl.copyWithoutModule
import kotlin.script.experimental.jvm.impl.scriptMetadataPath
import kotlin.script.experimental.jvm.impl.toBytes
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

// TODO: generate execution code (main)

open class BasicJvmScriptClassFilesGenerator(val outputDir: File) : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> {
        try {
            if (compiledScript !is KJvmCompiledScript<*>)
                return failure("Cannot generate classes: unsupported compiled script type $compiledScript")
            val module = (compiledScript.compiledModule as? KJvmCompiledModuleInMemory)
                ?: return failure("Cannot generate classes: unsupported module type ${compiledScript.compiledModule}")
            for ((path, bytes) in module.compilerOutputFiles) {
                File(outputDir, path).apply {
                    if (!parentFile.isDirectory) {
                        parentFile.mkdirs()
                    }
                    writeBytes(bytes)
                }
            }
            return ResultWithDiagnostics.Success(EvaluationResult(ResultValue.NotEvaluated, scriptEvaluationConfiguration))
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(
                e.asDiagnostics("Cannot generate script classes: ${e.message}", path = compiledScript.sourceLocationId)
            )
        }
    }
}

fun KJvmCompiledScript<*>.saveToJar(outputJar: File) {
    val module = (compiledModule as? KJvmCompiledModuleInMemory)
        ?: throw IllegalArgumentException("Unsupported module type $compiledModule")
    val dependenciesFromScript = compilationConfiguration[ScriptCompilationConfiguration.dependencies]
        ?.filterIsInstance<JvmDependency>()
        ?.flatMap { it.classpath }
        .orEmpty()
    val dependenciesForMain = scriptCompilationClasspathFromContext(
        KotlinPaths.Jar.ScriptingLib.baseName, KotlinPaths.Jar.ScriptingJvmLib.baseName,
        classLoader = this::class.java.classLoader,
        wholeClasspath = false
    )
    val dependencies = (dependenciesFromScript + dependenciesForMain).distinct()
    FileOutputStream(outputJar).use { fileStream ->
        val manifest = Manifest()
        manifest.mainAttributes.apply {
            putValue("Manifest-Version", "1.0")
            putValue("Created-By", "JetBrains Kotlin")
            if (dependencies.isNotEmpty()) {
                // TODO: implement options for various cases - paths as is (now), absolute paths (local execution only), names only (most likely as a hint only), fat jar
                putValue("Class-Path", dependencies.joinToString(" ") { it.toURI().toURL().toExternalForm() })
            }
            putValue("Main-Class", scriptClassFQName)
        }
        JarOutputStream(fileStream, manifest).use { jarStream ->
            jarStream.putNextEntry(JarEntry(scriptMetadataPath(scriptClassFQName)))
            jarStream.write(copyWithoutModule().toBytes())
            jarStream.closeEntry()
            for ((path, bytes) in module.compilerOutputFiles) {
                jarStream.putNextEntry(JarEntry(path))
                jarStream.write(bytes)
                jarStream.closeEntry()
            }
            jarStream.finish()
            jarStream.flush()
        }
        fileStream.flush()
    }
}

open class BasicJvmScriptJarGenerator(val outputJar: File) : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> {
        try {
            if (compiledScript !is KJvmCompiledScript<*>)
                return failure("Cannot generate jar: unsupported compiled script type $compiledScript")
            compiledScript.saveToJar(outputJar)
            return ResultWithDiagnostics.Success(EvaluationResult(ResultValue.NotEvaluated, scriptEvaluationConfiguration))
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(
                e.asDiagnostics("Cannot generate script jar: ${e.message}", path = compiledScript.sourceLocationId)
            )
        }
    }
}

private fun failure(msg: String) =
    ResultWithDiagnostics.Failure(msg.asErrorDiagnostics())

