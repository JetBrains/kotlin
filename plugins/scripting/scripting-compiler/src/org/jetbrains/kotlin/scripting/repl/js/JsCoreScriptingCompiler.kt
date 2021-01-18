/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.generateJsCode
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.serialization.js.ModuleKind
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.StringScriptSource

class JsCoreScriptingCompiler(
    private val environment: KotlinCoreEnvironment,
    private val nameTables: NameTables,
    private val symbolTable: SymbolTable,
    private val dependencyDescriptors: List<ModuleDescriptor>,
    private val replState: ReplCodeAnalyzerBase.ResettableAnalyzerState = ReplCodeAnalyzerBase.ResettableAnalyzerState()
) {
    fun compile(codeLine: ReplCodeLine): ReplCompileResult {
        val snippet = codeLine.code
        val snippetId = codeLine.no

        setIdeaIoUseFallback()

        val sourceCode = StringScriptSource(snippet, "line-$snippetId.kts")
        val snippetKtFile = getScriptKtFile(
            sourceCode,
            snippet,
            environment.project
        ).valueOr { return ReplCompileResult.Error(it.reports.joinToString { r -> r.message }) }

        val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] as MessageCollector
        val analyzerEngine = JsReplCodeAnalyzer(environment, dependencyDescriptors, replState)
        val analysisResult = analyzerEngine.analyzeReplLine(snippetKtFile, codeLine).also {
            AnalyzerWithCompilerReport.reportDiagnostics(it.bindingContext.diagnostics, messageCollector)
            if (messageCollector.hasErrors()) return ReplCompileResult.Error("Error while analysis")
        }

        val files = listOf(snippetKtFile)
        val (bindingContext, module) = analysisResult
        val psi2ir = Psi2IrTranslator(environment.configuration.languageVersionSettings, Psi2IrConfiguration())
        val psi2irContext = psi2ir.createGeneratorContext(module, bindingContext, symbolTable)
        val providers = generateTypicalIrProviderList(module, psi2irContext.irBuiltIns, psi2irContext.symbolTable)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, providers, emptyList(), null) // TODO: deserializer

        psi2irContext.irBuiltIns.let { irBuiltIns ->
            irBuiltIns.functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
        }

        val context = JsIrBackendContext(
            irModuleFragment.descriptor,
            psi2irContext.irBuiltIns,
            psi2irContext.symbolTable,
            irModuleFragment,
            emptySet(),
            environment.configuration,
            true
        )

        ExternalDependenciesGenerator(
            psi2irContext.symbolTable,
            generateTypicalIrProviderList(
                irModuleFragment.descriptor,
                psi2irContext.irBuiltIns,
                psi2irContext.symbolTable
            ),
            environment.configuration.languageVersionSettings
        ).generateUnboundSymbolsAsDependencies()

        environment.configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val code = generateJsCode(context, irModuleFragment, nameTables)

        return createCompileResult(LineId(codeLine.no, 0, codeLine.hashCode()), code)
    }
}
