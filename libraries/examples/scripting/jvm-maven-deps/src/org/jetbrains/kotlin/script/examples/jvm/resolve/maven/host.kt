/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import org.jetbrains.kotlin.script.util.*
import org.jetbrains.kotlin.script.util.impl.getResourcePathForClass
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.impl.KJVMCompilerImpl

val stdlibFile: File by lazy {
    KotlinJars.stdlib
            ?: throw Exception("Unable to find kotlin stdlib, please specify it explicitly via \"kotlin.java.stdlib.jar\" property")
}

val selfFile: File by lazy {
    getResourcePathForClass(MyScriptWithMavenDeps::class.java).takeIf(File::exists)
            ?: throw Exception("Unable to get path to the script base")
}

val scriptUtilsJarFile: File by lazy {
    getResourcePathForClass(DependsOn::class.java).takeIf(File::exists)
            ?: throw Exception("Unable to get path to the kotlin-script-util.jar")
}

class MyConfigurator(val baseClass: KClass<Any>? = null) : ScriptConfigurator {

    private val resolver = FilesAndMavenResolver()

    override suspend fun baseConfiguration(scriptSource: ScriptSource?) : ResultWithDiagnostics<ScriptCompileConfiguration> =
        myJvmConfig(scriptSource.toConfigEntry()).asSuccess()

    override suspend fun refineConfiguration(
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData
    ): ResultWithDiagnostics<ScriptCompileConfiguration> {
        val annotations = processedScriptData.getOrNull(ProcessedScriptDataParams.annotations)?.toList()?.takeIf { it.isNotEmpty() }
                ?: return configuration.asSuccess()
        val scriptContents = object : ScriptContents {
            override val annotations: Iterable<Annotation> = annotations
            override val file: File? = null
            override val text: CharSequence? = null
        }
        val diagnostics = arrayListOf<ScriptDiagnostic>()
        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(ScriptDiagnostic(message, mapLegacyDiagnosticSeverity(severity), mapLegacyScriptPosition(position)))
        }
        return try {
            val newDepsFromResolver = resolver.resolve(scriptContents, emptyMap(), ::report, null).get()
                    ?: return configuration.asSuccess(diagnostics)
            val resolvedClasspath = newDepsFromResolver.classpath.toList().takeIf { it.isNotEmpty() }
                    ?: return configuration.asSuccess(diagnostics)
            val newDependency = JvmDependency(resolvedClasspath)
            val updatedDeps =
                configuration.getOrNull(ScriptCompileConfigurationParams.dependencies)?.plus(newDependency) ?: listOf(newDependency)
            configuration.cloneWith(ScriptCompileConfigurationParams.dependencies to updatedDeps).asSuccess(diagnostics)
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics())
        }
    }
}

fun myJvmConfig(vararg params: Pair<TypedKey<*>, Any?>): ScriptCompileConfiguration =
    jvmConfigWithJavaHome(
        ScriptCompileConfigurationParams.scriptSignature to ScriptSignature(MyScriptWithMavenDeps::class, ProvidedDeclarations()),
        ScriptCompileConfigurationParams.importedPackages to listOf(DependsOn::class.qualifiedName!!, Repository::class.qualifiedName!!),
        ScriptCompileConfigurationParams.dependencies to listOf(
            JvmDependency(listOf(stdlibFile)),
            JvmDependency(listOf(selfFile)),
            JvmDependency(listOf(scriptUtilsJarFile))
        ),
        ScriptCompileConfigurationParams.updateConfigurationOnAnnotations to listOf(DependsOn::class, Repository::class),
        *params
    )

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(MyScriptWithMavenDeps::class)

    val host = JvmBasicScriptingHost(
        scriptDefinition.configurator,
        scriptCompiler,
        scriptDefinition.runner
    )

    return host.eval(myJvmConfig(scriptFile.toScriptSource().toConfigEntry()), ScriptEvaluationEnvironment())
}

fun main(vararg args: String) {
    if (args.size != 1) {
        println("usage: <app> <script file>")
    } else {
        val scriptFile = File(args[0])
        println("Executing script $scriptFile")

        val res = evalFile(scriptFile)

        res.reports.forEach {
            println(" : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
        }
    }
}
