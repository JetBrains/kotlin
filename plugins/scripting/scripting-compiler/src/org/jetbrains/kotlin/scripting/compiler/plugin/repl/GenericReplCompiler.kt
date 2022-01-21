/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
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
    ) : this(Disposer.newDisposable(), scriptDefinition, compilerConfiguration, messageCollector)

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
                ScriptDependenciesProvider.getInstance(checker.environment.project)?.getScriptConfiguration(psiFile)
                    ?.legacyDependencies
            var classpathAddendum: List<File>? = null
            if (compilerState.lastDependencies != newDependencies) {
                compilerState.lastDependencies = newDependencies
                classpathAddendum = newDependencies?.let { checker.environment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }
            }

            val analysisResult = compilerState.analyzerEngine.analyzeReplLine(psiFile, codeLine)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder, renderDiagnosticName = false)
            val scriptDescriptor = when (analysisResult) {
                is ReplCodeAnalyzerBase.ReplLineAnalysisResult.WithErrors -> {
                    return ReplCompileResult.Error(errorHolder.renderMessage())
                }
                is ReplCodeAnalyzerBase.ReplLineAnalysisResult.Successful -> {
                    (analysisResult.scriptDescriptor as? ScriptDescriptor)
                        ?: error("Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}")
                }
                else -> error("Unexpected result ${analysisResult::class.java}")
            }

            val generationState = GenerationState.Builder(
                psiFile.project,
                ClassBuilderFactories.BINARIES,
                compilerState.analyzerEngine.module,
                compilerState.analyzerEngine.trace.bindingContext,
                listOf(psiFile),
                compilerConfiguration
            ).build()

            generationState.scriptSpecific.earlierScriptsForReplInterpreter = compilerState.history.map { it.item }
            generationState.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                generationState,
                psiFile.script!!.containingKtFile.packageFqName,
                setOf(psiFile.script!!.containingKtFile)
            )

            compilerState.history.push(LineId(codeLine.no, 0, codeLine.hashCode()), scriptDescriptor)

            val classes = generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) }

            return ReplCompileResult.CompiledClasses(
                LineId(codeLine.no, 0, codeLine.hashCode()),
                compilerState.history.map { it.id },
                scriptDescriptor.name.identifier,
                classes,
                generationState.scriptSpecific.resultFieldName != null,
                classpathAddendum ?: emptyList(),
                generationState.scriptSpecific.resultTypeString ?: generationState.scriptSpecific.resultType?.let {
                    DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it)
                },
                null
            )
        }
    }

    companion object {
        private const val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}
