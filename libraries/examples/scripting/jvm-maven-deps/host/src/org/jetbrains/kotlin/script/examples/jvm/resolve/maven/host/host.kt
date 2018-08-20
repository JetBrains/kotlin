/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven.host

import org.jetbrains.kotlin.script.examples.jvm.resolve.maven.MyScriptWithMavenDeps
import org.jetbrains.kotlin.script.examples.jvm.resolve.maven.myJvmConfigParams
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.DummyCompiledJvmScriptCache
import kotlin.script.experimental.jvm.JvmBasicScriptingHost
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.script.experimental.jvm.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.impl.KJVMCompilerImpl
import kotlin.script.experimental.misc.*

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(
        ScriptingEnvironment(
            ScriptingEnvironmentProperties.baseClass<MyScriptWithMavenDeps>(),
            ScriptingEnvironmentProperties.getScriptingClass(JvmGetScriptingClass())
        )
    )

    val host = JvmBasicScriptingHost(
        scriptDefinition.compilationConfigurator,
        scriptCompiler,
        scriptDefinition.evaluator
    )

    return host.eval(scriptFile.toScriptSource(), ScriptCompileConfiguration(myJvmConfigParams), ScriptEvaluationEnvironment())
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
