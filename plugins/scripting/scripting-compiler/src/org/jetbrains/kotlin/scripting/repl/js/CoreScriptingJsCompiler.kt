/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.serialization.js.ModuleKind
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.StringScriptSource

class DeserializerWithDependencies(val deserializer: IrDeserializer, val dependencies: List<IrModuleFragment>)

class CoreScriptingJsCompiler(
    private val environment: KotlinCoreEnvironment,
    private val nameTables: NameTables,
    private val dependencyDescriptors: List<ModuleDescriptor>,
    private val createDeserializer: (ModuleDescriptor, SymbolTable, IrBuiltIns) -> DeserializerWithDependencies? = { _, _, _ -> null }
) {
    private val analyzerEngine: JsReplCodeAnalyzer = JsReplCodeAnalyzer(environment.project, dependencyDescriptors)
    private val symbolTable: SymbolTable = SymbolTable()

    fun compile(codeLine: ReplCodeLine): ReplCompileResult {
        val snippet = codeLine.code
        val snippetId = codeLine.no

        val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] as MessageCollector

        setIdeaIoUseFallback()

        val sourceCode = StringScriptSource(snippet, "line-$snippetId.kts")
        val snippetKtFile = getScriptKtFile(
            sourceCode,
            snippet,
            environment.project
        ).valueOr { return ReplCompileResult.Error(it.reports.joinToString { r -> r.message }) }

        analyzerEngine.analyzeReplLine(snippetKtFile, codeLine).also {
            AnalyzerWithCompilerReport.reportDiagnostics(it, messageCollector)
            if (messageCollector.hasErrors()) return ReplCompileResult.Error("Error while analysis")
        }

        val psi2ir = Psi2IrTranslator(environment.configuration.languageVersionSettings)
        val psi2irContext = psi2ir.createGeneratorContext(
            analyzerEngine.context.module,
            analyzerEngine.trace.bindingContext,
            symbolTable
        )

        val deserializerWithDependencies = createDeserializer(psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns)

        val irModuleFragment = psi2irContext.generateModuleFragment(listOf(snippetKtFile), deserializerWithDependencies?.deserializer)

        val deserializedFragments = deserializerWithDependencies?.dependencies ?: emptyList()

        val irFiles = sortDependencies(deserializedFragments).flatMap { it.files } + irModuleFragment.files
        irModuleFragment.files.clear()
        irModuleFragment.files += irFiles


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
            irModuleFragment.descriptor,
            psi2irContext.symbolTable,
            psi2irContext.irBuiltIns,
            deserializer = deserializerWithDependencies?.deserializer
        ).generateUnboundSymbolsAsDependencies()

        with(context.implicitDeclarationFile) {
            if (!irModuleFragment.files.contains(this)) {
                irModuleFragment.files += this
            }
        }
        context.implicitDeclarationFile.declarations.clear()

        environment.configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val code = compileForRepl(
            context,
            irModuleFragment,
            nameTables
        )

        return createCompileResult(
            LineId(codeLine),
            code
        )
    }
}
