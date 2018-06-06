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
import kotlin.script.experimental.misc.*

val myJvmConfigParams = jvmJavaHomeParams + with(ScriptCompileConfigurationProperties) {
    listOf(
        baseClass<MyScript>(),
        dependencies(JvmDependency(scriptCompilationClasspathFromContext("scripting-jvm-simple-script" /* script library jar name */)))
    )
}

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptCompiler = JvmScriptCompiler(KJVMCompilerImpl(), DummyCompiledJvmScriptCache())
    val scriptDefinition = ScriptDefinitionFromAnnotatedBaseClass(
        ScriptingEnvironment(
            ScriptingEnvironmentProperties.baseClass<MyScript>(),
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
