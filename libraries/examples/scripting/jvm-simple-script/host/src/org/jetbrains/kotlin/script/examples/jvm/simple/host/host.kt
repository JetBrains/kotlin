/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple.host

import org.jetbrains.kotlin.script.examples.jvm.simple.MyScript
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.createScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment
import kotlin.script.experimental.jvm.jvmDependenciesFromCurrentContext
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptDefinition = createScriptDefinitionFromAnnotatedBaseClass(
        KotlinType(MyScript::class),
        defaultJvmScriptingEnvironment
    )
    val additionalCompilationProperties = buildScriptingProperties {
        jvmDependenciesFromCurrentContext(
            "scripting-jvm-simple-script" /* script library jar name */
        )
    }

    val host = BasicJvmScriptingHost()

    return host.eval(
        scriptFile.toScriptSource(), scriptDefinition, additionalCompilationProperties, ScriptEvaluationEnvironment()
    )
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
