/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite

import com.intellij.openapi.project.Project
import org.jetbrains.kaptlite.diagnostic.DefaultErrorMessagesKaptLite
import org.jetbrains.kaptlite.diagnostic.ErrorsKaptLite
import org.jetbrains.kaptlite.stubs.GeneratorOutput
import org.jetbrains.kaptlite.stubs.StubGenerator
import org.jetbrains.kaptlite.stubs.util.CodeScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import java.io.PrintStream
import kotlin.system.measureTimeMillis

class KaptLiteAnalysisHandlerExtension(
    configuration: CompilerConfiguration,
    private val options: KaptLiteOptions,
    override val messageCollector: MessageCollector
) : AbstractKaptLiteAnalysisHandlerExtension(configuration) {
    override fun getOutput(state: GenerationState): GeneratorOutput = RealFileGeneratorOutput(state, options.stubsOutputDir)

    private class RealFileGeneratorOutput(private val state: GenerationState, private val outputDir: File) : GeneratorOutput {
        private val messageCollector = state.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        override fun produce(internalName: String, path: String, block: CodeScope.() -> Unit) {
            val file = File(outputDir, path)
            file.parentFile.mkdirs()
            file.outputStream().use { os ->
                val scope = CodeScope(PrintStream(os))
                scope.block()
            }

            if (state.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)) {
                val sourceFiles = state.factory["$internalName.class"]?.sourceFiles ?: emptyList()
                if (sourceFiles.isNotEmpty()) {
                    messageCollector.report(CompilerMessageSeverity.OUTPUT, OutputMessageUtil.formatOutputMessage(sourceFiles, file))
                }
            }
        }
    }
}

abstract class AbstractKaptLiteAnalysisHandlerExtension(private val configuration: CompilerConfiguration) : AnalysisHandlerExtension {
    abstract val messageCollector: MessageCollector

    abstract fun getOutput(state: GenerationState): GeneratorOutput

    override fun analysisCompleted(
        project: Project, module: ModuleDescriptor,
        bindingTrace: BindingTrace, files: Collection<KtFile>
    ): AnalysisResult? {
        if (DiagnosticUtils.hasError(bindingTrace.bindingContext.diagnostics)) {
            return AnalysisResult.compilationError(bindingTrace.bindingContext)
        }

        if (!generateStubs(project, module, bindingTrace.bindingContext, files.toList())) {
            return AnalysisResult.compilationError(bindingTrace.bindingContext)
        }
        return null
    }

    private fun generateStubs(
        project: Project,
        module: ModuleDescriptor,
        bindingContext: BindingContext,
        files: List<KtFile>
    ): Boolean {
        val builderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT_LITE)

        val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME] ?: module.name.asString()
        val targetId = TargetId(moduleName, "java-production")

        val generationState = GenerationState.Builder(project, builderFactory, module, bindingContext, files, configuration)
            .targetId(targetId)
            .isIrBackend(false)
            .build()

        val diagnosticsTrace = DelegatingBindingTrace(bindingContext, "For kapt-lite diagnostics in ${this::class.java}", false)

        try {
            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

            val time = measureTimeMillis {
                val compiledClasses = builderFactory.compiledClasses
                val origins = builderFactory.origins
                StubGenerator(diagnosticsTrace, compiledClasses, origins, generationState).use { stubGenerator ->
                    stubGenerator.generate(getOutput(generationState))
                }
            }

            diagnosticsTrace.reportFromPlugin(ErrorsKaptLite.TIME.on(files[0], "${time}ms"), DefaultErrorMessagesKaptLite)
        } finally {
            generationState.destroy()
        }

        return !AnalyzerWithCompilerReport.Companion.reportDiagnostics(diagnosticsTrace.bindingContext.diagnostics, messageCollector)
    }
}