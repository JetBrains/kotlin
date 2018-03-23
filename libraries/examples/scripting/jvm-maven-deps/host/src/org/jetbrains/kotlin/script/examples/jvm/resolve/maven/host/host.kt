/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven.host

import org.jetbrains.kotlin.script.examples.jvm.resolve.maven.MyScriptWithMavenDeps
import org.jetbrains.kotlin.script.examples.jvm.resolve.maven.myJvmConfig
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationEnvironment
import kotlin.script.experimental.api.toConfigEntry
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.DummyCompiledJvmScriptCache
import kotlin.script.experimental.jvm.JvmBasicScriptingHost
import kotlin.script.experimental.jvm.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.impl.KJVMCompilerImpl

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(MyScriptWithMavenDeps::class)

    val host = JvmBasicScriptingHost(
        scriptDefinition.configurator,
        scriptCompiler,
        scriptDefinition.runner
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
