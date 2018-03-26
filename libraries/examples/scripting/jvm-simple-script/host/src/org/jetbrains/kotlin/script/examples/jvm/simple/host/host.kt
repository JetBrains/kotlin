/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple.host

import org.jetbrains.kotlin.script.examples.jvm.simple.MyScript
import org.jetbrains.kotlin.script.util.*
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.impl.KJVMCompilerImpl

inline fun myJvmConfig(
    from: HeterogeneousMap = HeterogeneousMap(),
    crossinline body: JvmScriptCompileConfigurationParams.Builder.() -> Unit = {}
) = jvmConfigWithJavaHome(from) {
    signature<MyScript>()
    dependencies(scriptCompilationClasspathFromContext("script" /* script library jar name */))
    body()
}

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(
        ScriptingEnvironment(ScriptingEnvironmentParams.baseClass to MyScript::class)
    )

    val host = JvmBasicScriptingHost(
        scriptDefinition.compilationConfigurator,
        scriptCompiler,
        scriptDefinition.evaluator
    )

    return host.eval(myJvmConfig { add(scriptFile.toScriptSource().toConfigEntry()) }, ScriptEvaluationEnvironment())
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
