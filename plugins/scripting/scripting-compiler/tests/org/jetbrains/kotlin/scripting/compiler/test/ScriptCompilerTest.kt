/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import junit.framework.TestCase
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptCompilerTest : TestCase() {

    fun testCompilationWithRefinementError() {
        val res = compile("nonsense".toScriptSource()) {
            refineConfiguration {
                beforeCompiling {
                    ResultWithDiagnostics.Failure("err13".asErrorDiagnostics())
                }
            }
        }

        assertTrue(res is ResultWithDiagnostics.Failure)
        assertTrue(res.reports.any { it.message == "err13" })
        assertTrue(res.reports.none { it.message.contains("nonsense") })
    }

    fun compile(
        script: SourceCode,
        cfgBody: ScriptCompilationConfiguration.Builder.() -> Unit
    ): ResultWithDiagnostics<CompiledScript> {
        val compilationConfiguration = ScriptCompilationConfiguration(cfgBody)
        val compiler = ScriptJvmCompilerIsolated(defaultJvmScriptingHostConfiguration)
        return compiler.compile(script, compilationConfiguration)
    }
}