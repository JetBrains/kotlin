/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.script.experimental.jvmhost.impl

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
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.KJvmCompilerProxy

class KJvmCompilerImpl(val hostConfiguration: ScriptingHostConfiguration) : KJvmCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> =
        withMessageCollectorAndDisposable(locationId = script.locationId) { messageCollector, disposable ->

            val context = createSharedCompilationContext(scriptCompilationConfiguration, hostConfiguration, messageCollector, disposable)

            val mainKtFile =
                getScriptKtFile(script, context.baseScriptCompilationConfiguration, context.environment.project, messageCollector)
                    .resultOr { return it }

            context.scriptCompilationState.configureFor(script, context.baseScriptCompilationConfiguration)

            val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(context, mainKtFile, messageCollector)

            val analysisResult = analyze(sourceFiles, context.environment)

            if (!analysisResult.shouldGenerateCode) return failure(script, messageCollector, "no code to generate")
            if (analysisResult.isError() || messageCollector.hasErrors()) return failure(messageCollector)

            val generationState = generate(analysisResult, sourceFiles, context.environment.configuration)

            val compiledScript =
                makeCompiledScript(generationState, script, sourceFiles.first(), sourceDependencies) { ktFile ->
                    context.scriptCompilationState.configurations.entries.find { ktFile.name == it.key.name }?.value
                        ?: context.baseScriptCompilationConfiguration
                }

            ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
        }
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
