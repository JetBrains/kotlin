/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple.host

import org.jetbrains.kotlin.script.examples.jvm.simple.MyScript
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompileConfiguration
import kotlin.script.experimental.api.ScriptEvaluationEnvironment
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.makeBasicHostFromAnnotatedScriptBaseClass

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val additionalCompilationProperties = ScriptCompileConfiguration.create {
        jvm {
            dependenciesFromCurrentContext(
                "scripting-jvm-simple-script" /* script library jar name */
            )
        }
    }

    val host = makeBasicHostFromAnnotatedScriptBaseClass<MyScript>()

    return host.eval(scriptFile.toScriptSource(), additionalCompilationProperties, null)
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
