/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import kotlin.script.experimental.api.*

class JsScriptCompiler(
    environment: KotlinCoreEnvironment
) : ScriptCompiler {
    private val nameTables = NameTables(emptyList())
    private val dependencies: List<ModuleDescriptor> = readLibrariesFromConfiguration(environment.configuration)
    private val compiler = CoreScriptingJsCompiler(environment, nameTables, dependencies)

    val scriptDependencyBinary = ScriptDependencyCompiler(environment, nameTables).compile(dependencies).first

    override suspend fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val compileResult = compiler.compile(makeReplCodeLine(0, script.text))
        return when (compileResult) {
            is CompiledClasses -> ResultWithDiagnostics.Success(
                CompiledToJsScript(compileResult.data as String, scriptCompilationConfiguration)
            )
            is Incomplete -> ResultWithDiagnostics.Failure(
                ScriptDiagnostic("Incomplete code")
            )
            is Error -> ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    message = compileResult.message,
                    severity = ScriptDiagnostic.Severity.ERROR
                )
            )
        }
    }
}
