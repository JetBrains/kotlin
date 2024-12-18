/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.scriptResultFieldDataAttr
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

// WARNING: not thread safe, assuming external synchronization

open class GenericReplCompiler(
    disposable: Disposable,
    scriptDefinition: KotlinScriptDefinition,
    private val compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector
) : ReplCompiler {

    constructor(
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
    ) : this(
        Disposer.newDisposable("Default disposable for ${GenericReplCompiler::class.simpleName}"),
        scriptDefinition,
        compilerConfiguration,
        messageCollector
    )

    private val checker =
        GenericReplChecker(
            disposable,
            scriptDefinition,
            compilerConfiguration,
            messageCollector
        )

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
        GenericReplCompilerState(checker.environment, lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = checker.check(state, codeLine)

    @OptIn(ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)
    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        state.lock.write {
            val compilerState = state.asState(GenericReplCompilerState::class.java)

            val (psiFile, errorHolder) = run {
                if (compilerState.lastLineState == null || compilerState.lastLineState!!.codeLine != codeLine) {
                    val res = checker.check(state, codeLine)
                    when (res) {
                        is ReplCheckResult.Incomplete -> return@compile ReplCompileResult.Incomplete("Code is incomplete")
                        is ReplCheckResult.Error -> return@compile ReplCompileResult.Error(res.message, res.location)
                        is ReplCheckResult.Ok -> {
                        } // continue
                    }
                }
                Pair(compilerState.lastLineState!!.psiFile, compilerState.lastLineState!!.errorHolder)
            }

            @Suppress("DEPRECATION")
            val newDependencies =
                ScriptConfigurationsProvider.getInstance(checker.environment.project)?.getScriptConfiguration(psiFile)
                    ?.legacyDependencies
            var classpathAddendum: List<File>? = null
            if (compilerState.lastDependencies != newDependencies) {
                compilerState.lastDependencies = newDependencies
                classpathAddendum = newDependencies?.let { checker.environment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }
            }

            val analysisResult = compilerState.analyzerEngine.analyzeReplLine(psiFile, codeLine)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder, renderDiagnosticName = false)
            val scriptDescriptor = when (analysisResult) {
                is ReplCodeAnalyzerBase.ReplLineAnalysisResult.Successful -> {
                    (analysisResult.scriptDescriptor as? ScriptDescriptor)
                        ?: error("Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}")
                }
                is ReplCodeAnalyzerBase.ReplLineAnalysisResult.WithErrors -> {
                    return ReplCompileResult.Error(errorHolder.renderMessage())
                }
                else -> error("Unexpected result ${analysisResult::class.java}")
            }

            val generationState = GenerationState(psiFile.project, compilerState.analyzerEngine.module, compilerConfiguration)

            val generatorExtensions =
                object : JvmGeneratorExtensionsImpl(checker.environment.configuration) {
                    override fun getPreviousScripts() = compilerState.history.map { compilerState.symbolTable.descriptorExtension.referenceScript(it.item) }
                }
            val codegenFactory = JvmIrCodegenFactory(
                checker.environment.configuration,
                compilerState.mangler, compilerState.symbolTable, generatorExtensions
            )

            val irBackendInput = codegenFactory.convertToIr(
                generationState, listOf(psiFile), compilerState.analyzerEngine.trace.bindingContext,
            )

            codegenFactory.generateModule(generationState, irBackendInput)

            compilerState.history.push(LineId(codeLine.no, 0, codeLine.hashCode()), scriptDescriptor)

            val classes = generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) }

            val resultType = irBackendInput.irModuleFragment.files.singleOrNull()?.declarations?.singleOrNull()?.let {
                (it as? IrClass)?.scriptResultFieldDataAttr?.fieldTypeName
            }

            return ReplCompileResult.CompiledClasses(
                LineId(codeLine.no, 0, codeLine.hashCode()),
                compilerState.history.map { it.id },
                scriptDescriptor.name.identifier,
                classes,
                resultType != null,
                classpathAddendum ?: emptyList(),
                resultType,
                null
            )
        }
    }

    companion object {
        private const val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}
