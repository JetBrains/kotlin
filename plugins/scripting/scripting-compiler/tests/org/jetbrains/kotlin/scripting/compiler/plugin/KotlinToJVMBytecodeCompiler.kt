/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase.getSourceFiles
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase.writeOutputsIfNeeded
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.LOOKUP_TRACKER
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K1JvmIrCodegenFactory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.K1JvmBackendClassResolverForModuleWithDependencies
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

internal object KotlinToJVMBytecodeCompiler {
    private fun compileModules(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
    ): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val compilerConfiguration = environment.configuration

        val project = environment.project
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = compilerConfiguration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        check(compilerConfiguration.useFir == false)
        val diagnosticsReporter = DiagnosticsCollectorImpl()
        val backendInputForMultiModuleChunk =
            runFrontendAndGenerateIrUsingClassicFrontend(environment, compilerConfiguration, chunk, diagnosticsReporter) ?: return true

        return backendInputForMultiModuleChunk.runBackend(
            project,
            chunk,
            compilerConfiguration,
            diagnosticsReporter,
            buildFile,
            allSourceFiles = environment.getSourceFiles(),
        )
    }

    private fun BackendInputForMultiModuleChunk.runBackend(
        project: Project,
        chunk: List<Module>,
        compilerConfiguration: CompilerConfiguration,
        diagnosticsReporter: BaseDiagnosticsCollector,
        buildFile: File?,
        allSourceFiles: List<KtFile>?,
    ): Boolean {
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val codegenInputs = ArrayList<JvmIrCodegenFactory.CodegenInput>(chunk.size)
        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = if (allSourceFiles != null) {
                val sourceFiles = module.getSourceFiles(allSourceFiles, localFileSystem, chunk.size > 1, buildFile)
                if (!checkKotlinPackageUsageForPsi(compilerConfiguration, sourceFiles)) return false
                sourceFiles
            } else null

            val moduleConfiguration = compilerConfiguration.createConfigurationForModule(module, buildFile)
            val backendInput = (if (ktFiles != null) {
                codegenFactory.normalFactory.getModuleChunkBackendInput(backendInput, ktFiles)
            } else {
                val wholeModule = backendInput.irModuleFragment
                val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor)
                wholeModule.files.filterTo(moduleCopy.files) { file ->
                    file.fileEntry.name in module.getSourceFiles()
                }
                backendInput.copy(moduleCopy)
            }).let {
                if (firJvmBackendExtension != null) {
                    it.copy(backendExtension = firJvmBackendExtension)
                } else it
            }
            // Lowerings (per module)
            codegenInputs += JvmBackendPipelinePhase.runLowerings(
                project,
                moduleConfiguration,
                moduleDescriptor,
                module,
                codegenFactory.normalFactory,
                backendInput,
                diagnosticsReporter,
                K1JvmBackendClassResolverForModuleWithDependencies(moduleDescriptor),
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            // Codegen (per module)
            outputs += JvmBackendPipelinePhase.runCodegen(
                input,
                input.state,
                codegenFactory.normalFactory,
                diagnosticsReporter,
                compilerConfiguration,
                reportDiagnosticsToMessageCollector = true
            )
        }

        return writeOutputsIfNeeded(
            project,
            compilerConfiguration,
            hasPendingErrors = false,
            outputs,
            mainClassFqName
        )
    }

    private fun runFrontendAndGenerateIrUsingClassicFrontend(
        environment: KotlinCoreEnvironment,
        compilerConfiguration: CompilerConfiguration,
        chunk: List<Module>,
        diagnosticsReporter: DiagnosticReporter,
    ): BackendInputForMultiModuleChunk? {
        // K1: Frontend
        val result = environment.configuration.perfManager.let {
            it?.notifyPhaseFinished(PhaseType.Initialization)
            it.tryMeasurePhaseTime(PhaseType.Analysis) {
                repeatAnalysisIfNeeded(analyze(environment), environment)
            }
        }
        if (result == null || !result.shouldGenerateCode) return null

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val mainClassFqName = runIf(chunk.size == 1 && compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(
                result.bindingContext, compilerConfiguration.languageVersionSettings, environment.getSourceFiles()
            )
        }

        // K1: PSI2IR
        val [factory, input] = convertToIr(environment, result, diagnosticsReporter)
        return BackendInputForMultiModuleChunk(factory, input, result.moduleDescriptor, mainClassFqName = mainClassFqName)
    }

    private data class BackendInputForMultiModuleChunk(
        val codegenFactory: K1JvmIrCodegenFactory,
        val backendInput: JvmIrCodegenFactory.BackendInput,
        val moduleDescriptor: ModuleDescriptor,
        val firJvmBackendExtension: FirJvmBackendExtension? = null,
        val mainClassFqName: FqName? = null,
    )

    @K1Deprecation
    fun compileBunchOfSources(environment: KotlinCoreEnvironment): Boolean {
        val module = ModuleBuilder("test", environment.configuration.outputDirectory!!.path, "test")
        return compileModules(environment, buildFile = null, listOf(module))
    }

    private fun repeatAnalysisIfNeeded(result: AnalysisResult?, environment: KotlinCoreEnvironment): AnalysisResult? {
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            val configuration = environment.configuration

            val oldReadOnlyValue = configuration.isReadOnly
            configuration.isReadOnly = false
            configuration.addJavaSourceRoots(result.additionalJavaRoots)
            configuration.isReadOnly = oldReadOnlyValue

            if (result.addToEnvironment) {
                environment.updateClasspath(result.additionalJavaRoots.map { JavaSourceRoot(it, null) })
            }

            if (result.additionalClassPathRoots.isNotEmpty()) {
                environment.updateClasspath(result.additionalClassPathRoots.map { JvmClasspathRoot(it, false) })
            }

            if (result.additionalKotlinRoots.isNotEmpty()) {
                environment.addKotlinSourceRoots(result.additionalKotlinRoots)
            }

            KotlinJavaPsiFacade.getInstance(environment.project).clearPackageCaches()

            val lookupTracker = configuration[LOOKUP_TRACKER]
            lookupTracker?.clear()

            // Clear all diagnostic messages
            @OptIn(MessageCollectorAccess::class)
            configuration.messageCollector.clear()

            // Repeat analysis with additional source roots generated by compiler plugins.
            return repeatAnalysisIfNeeded(analyze(environment), environment)
        }

        return result
    }

    private fun convertToIr(
        environment: KotlinCoreEnvironment,
        result: AnalysisResult,
        diagnosticsReporter: DiagnosticReporter,
    ): Pair<K1JvmIrCodegenFactory, JvmIrCodegenFactory.BackendInput> {
        val configuration = environment.configuration
        val codegenFactory = K1JvmIrCodegenFactory(configuration)
        val backendInput = environment.configuration.perfManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            codegenFactory.convertToIr(
                environment.getSourceFiles(),
                configuration,
                result.moduleDescriptor,
                diagnosticsReporter,
                result.bindingContext,
                configuration.languageVersionSettings,
                ignoreErrors = false,
                skipBodies = false,
            )
        }
        return Pair(codegenFactory, backendInput)
    }

    private fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val sourceFiles = environment.getSourceFiles()

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(environment.configuration)
        analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
            val project = environment.project
            val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)?.mapNotNullTo(hashSetOf()) { module ->
                environment.findLocalFile(module.getOutputDirectory())
            }.orEmpty()
            val sourcesOnly = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            // To support partial and incremental compilation, we add the scope which contains binaries from output directories
            // of the compiled modules (.class) to the list of scopes of the source module
            val scope = if (moduleOutputs.isEmpty()) sourcesOnly else sourcesOnly.uniteWith(
                VfsBasedProjectEnvironment.DirectoriesScope(
                    project,
                    moduleOutputs
                )
            )
            @Suppress("DEPRECATION_ERROR")
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                sourceFiles,
                NoScopeRecordCliBindingTrace(project),
                environment.configuration,
                environment::createPackagePartProvider,
                sourceModuleSearchScope = scope,
            )
        }

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalRoots)
            analysisResult
        else
            null
    }

    private fun findMainClass(bindingContext: BindingContext, languageVersionSettings: LanguageVersionSettings, files: List<KtFile>): FqName? {
        val mainFunctionDetector = MainFunctionDetector(bindingContext, languageVersionSettings)
        return files.asSequence()
            .map { file ->
                mainFunctionDetector.findMainFunction(file)?.let { mainFunction ->
                    if (mainFunction.isTopLevel) {
                        JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
                    } else {
                        val parent = mainFunction.getParentOfType<KtClassOrObject>(strict = true)
                        if (parent is KtObjectDeclaration && parent.isCompanion()) {
                            mainFunction.fqName?.parent()?.parent()
                        } else {
                            mainFunction.fqName?.parent()
                        }
                    }
                }
            }
            .singleOrNull { it != null }
    }

    private fun MainFunctionDetector.findMainFunction(container: KtDeclarationContainer): KtNamedFunction? =
        container.declarations.mapNotNull { declaration ->
            when (declaration) {
                is KtNamedFunction -> declaration.takeIf(::isMain)
                is KtDeclarationContainer -> findMainFunction(declaration)
                else -> null
            }
        }.singleOrNull()
}
