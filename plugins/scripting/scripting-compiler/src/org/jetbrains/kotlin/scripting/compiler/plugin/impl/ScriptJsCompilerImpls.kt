/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.repl.js.JsCompiledScript
import org.jetbrains.kotlin.scripting.repl.js.JsCoreScriptingCompiler
import org.jetbrains.kotlin.scripting.repl.js.JsScriptDependencyCompiler
import org.jetbrains.kotlin.scripting.repl.js.readLibrariesFromConfiguration
import kotlin.script.experimental.api.*

class JsScriptCompilerWithDependenciesProxy(private val environment: KotlinCoreEnvironment) : ScriptCompilerProxy {
    private val nameTables = NameTables(emptyList(), mappedNames = mutableMapOf())
    private val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc /* TODO */), IrFactoryImpl)
    private val dependencies: List<ModuleDescriptor> = readLibrariesFromConfiguration(environment.configuration)
    private val compiler = JsCoreScriptingCompiler(environment, nameTables, symbolTable, dependencies)
    private var scriptDependencyCompiler: JsScriptDependencyCompiler? =
        JsScriptDependencyCompiler(environment.configuration, nameTables, symbolTable)

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> {
        val parentMessageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]
        return withMessageCollector(script = script, parentMessageCollector = parentMessageCollector) { messageCollector ->
            environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            try {
                val dependenciesCode = scriptDependencyCompiler?.let { scriptDependencyCompiler = null; it.compile(dependencies) } ?: ""
                when (val compileResult = compiler.compile(org.jetbrains.kotlin.scripting.repl.js.makeReplCodeLine(0, script.text))) {
                    is ReplCompileResult.CompiledClasses -> {
                        val compileJsCode = compileResult.data as String
                        ResultWithDiagnostics.Success(
                            JsCompiledScript(dependenciesCode + "\n" + compileJsCode, scriptCompilationConfiguration)
                        )
                    }
                    is ReplCompileResult.Incomplete -> ResultWithDiagnostics.Failure(
                        ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code")
                    )
                    is ReplCompileResult.Error -> ResultWithDiagnostics.Failure(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError,
                            message = compileResult.message,
                            severity = ScriptDiagnostic.Severity.ERROR
                        )
                    )
                }
            } finally {
                if (parentMessageCollector != null)
                    environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, parentMessageCollector)
            }
        }
    }
}
