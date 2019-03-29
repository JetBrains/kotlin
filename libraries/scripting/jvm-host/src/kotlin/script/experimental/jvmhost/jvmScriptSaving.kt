/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvmhost.impl.KJvmCompiledScript

// TODO: generate execution code (main)

open class BasicJvmScriptClassFilesGenerator(val outputDir: File) : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> {
        try {
            if (compiledScript !is KJvmCompiledScript<*>)
                return failure("Unsupported compiled script type ${compiledScript::class.qualifiedName}")
            val module = compiledScript.compiledModule ?: return failure("Cannot generate classes: module is null")
            for ((path, bytes) in module.compilerOutputFiles) {
                File(outputDir, path).apply {
                    if (!parentFile.isDirectory) {
                        parentFile.mkdirs()
                    }
                    writeBytes(bytes)
                }
            }
            return ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Unit, scriptEvaluationConfiguration))
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(
                e.asDiagnostics("Error saving compiled script classes", path = compiledScript.sourceLocationId)
            )
        }
    }
}


open class BasicJvmScriptJarGenerator(val outputJar: File) : ScriptEvaluator {

    override suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> {
        try {
            if (compiledScript !is KJvmCompiledScript<*>)
                return failure("Unsupported compiled script type ${compiledScript::class.qualifiedName}")
            val module = compiledScript.compiledModule ?: return failure("Cannot generate classes: module is null")
            val dependencies = compiledScript.compilationConfiguration[ScriptCompilationConfiguration.dependencies]
                ?.filterIsInstance<JvmDependency>()
                ?.flatMap { it.classpath }
                .orEmpty()
            FileOutputStream(outputJar).use { fileStream ->
                val manifest = Manifest()
                manifest.mainAttributes.apply {
                    putValue("Manifest-Version", "1.0")
                    putValue("Created-By", "JetBrains Kotlin")
                    if (dependencies.isNotEmpty()) {
                        putValue("Class-Path", dependencies.joinToString(" ") { it.name })
                    }
                }
                // TODO: fat jar/dependencies
                val jarStream = JarOutputStream(fileStream, manifest)
                for ((path, bytes) in module.compilerOutputFiles) {
                    jarStream.putNextEntry(JarEntry(path))
                    jarStream.write(bytes)
                }
                jarStream.finish()
            }
            return ResultWithDiagnostics.Success(EvaluationResult(ResultValue.Unit, scriptEvaluationConfiguration))
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(
                e.asDiagnostics("Error saving compiled script jar", path = compiledScript.sourceLocationId)
            )
        }
    }
}

private fun failure(msg: String) =
    ResultWithDiagnostics.Failure(msg.asErrorDiagnostics())

