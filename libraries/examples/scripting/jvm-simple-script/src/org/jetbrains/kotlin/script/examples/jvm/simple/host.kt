/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple

import org.jetbrains.kotlin.script.util.KotlinJars
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
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
    PathUtil.getResourcePathForClass(MyScript::class.java).takeIf(File::exists)
            ?: throw Exception("Unable to get path to the script base")
}

fun myJvmConfig(vararg params: Pair<TypedKey<*>, Any?>): ScriptCompileConfiguration =
    jvmConfigWithJavaHome(
        ScriptCompileConfigurationParams.scriptSignature to ScriptSignature(MyScript::class, ProvidedDeclarations.Empty),
        ScriptCompileConfigurationParams.dependencies to listOf(
            JvmDependency(listOf(stdlibFile)),
            JvmDependency(listOf(selfFile))
        ),
        *params
    )


fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(MyScript::class)

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
