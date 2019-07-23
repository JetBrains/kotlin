/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptJvmCompilerProxy
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration

class ScriptJvmCompilerIsolated(val hostConfiguration: ScriptingHostConfiguration) : ScriptJvmCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> =
        withMessageCollectorAndDisposable(script = script) { messageCollector, disposable ->

            val context = createIsolatedCompilationContext(
                scriptCompilationConfiguration, hostConfiguration, messageCollector, disposable
            )

            compileImpl(script, context, messageCollector)
        }
}

class ScriptJvmCompilerFromEnvironment(val environment: KotlinCoreEnvironment) : ScriptJvmCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val parentMessageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]
        return withMessageCollector(script = script, parentMessageCollector = parentMessageCollector) { messageCollector ->

            val context = createCompilationContextFromEnvironment(scriptCompilationConfiguration, environment, messageCollector)

            try {
                environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

                compileImpl(script, context, messageCollector)

            } finally {
                if (parentMessageCollector != null)
                    environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, parentMessageCollector)
            }
        }
    }
}

private fun compileImpl(
    script: SourceCode,
    context: SharedScriptCompilationContext,
    messageCollector: ScriptDiagnosticsMessageCollector
): ResultWithDiagnostics<CompiledScript<*>> {
    val mainKtFile =
        getScriptKtFile(
            script,
            context.baseScriptCompilationConfiguration,
            context.environment.project,
            messageCollector
        )
            .valueOr { return it }

    val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(
        context,
        mainKtFile,
        messageCollector
    )

    val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
    val analysisResult = analyze(sourceFiles, context.environment)

    if (!analysisResult.shouldGenerateCode) return failure(
        script,
        messageCollector,
        "no code to generate"
    )
    if (analysisResult.isError() || messageCollector.hasErrors()) return failure(
        messageCollector
    )

    val generationState =
        generate(analysisResult, sourceFiles, context.environment.configuration)

    val compiledScript =
        makeCompiledScript(
            generationState,
            script,
            sourceFiles.first(),
            sourceDependencies
        ) { ktFile ->
            dependenciesProvider?.getScriptConfigurationResult(ktFile)?.valueOrNull()?.configuration
                ?: context.baseScriptCompilationConfiguration
        }

    return ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
}

private fun analyze(sourceFiles: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
    val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector, environment.configuration.languageVersionSettings)

    analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
        val project = environment.project
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project,
            sourceFiles,
            NoScopeRecordCliBindingTrace(),
            environment.configuration,
            environment::createPackagePartProvider
        )
    }
    return analyzerWithCompilerReport.analysisResult
}

private fun generate(
    analysisResult: AnalysisResult, sourceFiles: List<KtFile>, kotlinCompilerConfiguration: CompilerConfiguration
): GenerationState {
    val generationState = GenerationState.Builder(
        sourceFiles.first().project,
        ClassBuilderFactories.BINARIES,
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        sourceFiles,
        kotlinCompilerConfiguration
    ).build()

    KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
    return generationState
}
