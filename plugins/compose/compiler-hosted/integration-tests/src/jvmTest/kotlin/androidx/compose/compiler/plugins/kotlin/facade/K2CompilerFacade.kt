/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.facade

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.metadataVersion

class FirAnalysisResult(
    val firResult: FirResult,
    override val files: List<KtFile>,
    val reporter: BaseDiagnosticsCollector
) : AnalysisResult {
    override val diagnostics: Map<String, List<AnalysisResult.Diagnostic>>
        get() = reporter.diagnostics.groupBy(
            keySelector = { it.psiElement.containingFile.name },
            valueTransform = { AnalysisResult.Diagnostic(it.factoryName, it.textRanges) }
        )
}

private class FirFrontendResult(
    val firResult: Fir2IrActualizedResult,
    val generatorExtensions: JvmGeneratorExtensions,
)

class K2CompilerFacade(environment: KotlinCoreEnvironment) : KotlinCompilerFacade(environment) {
    private val project: Project
        get() = environment.project

    private val configuration: CompilerConfiguration
        get() = environment.configuration

    private fun createSourceSession(
        moduleData: FirModuleData,
        projectSessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment
    ): FirSession {
        return FirJvmSessionFactory.createModuleBasedSession(
            moduleData,
            projectSessionProvider,
            PsiBasedProjectFileSearchScope(
                TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(
                    project
                )
            ),
            projectEnvironment,
            { null },
            FirExtensionRegistrar.getInstances(project),
            configuration.languageVersionSettings,
            configuration.get(JVMConfigurationKeys.JVM_TARGET) ?: error("JVM_TARGET is not specified in compiler configuration"),
            configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
            configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER),
            configuration.get(CommonConfigurationKeys.IMPORT_TRACKER),
            predefinedJavaComponents = null,
            needRegisterJavaElementFinder = true,
            registerExtraComponents = {},
            init = {}
        )
    }

    override fun analyze(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): FirAnalysisResult {
        val rootModuleName = configuration.get(CommonConfigurationKeys.MODULE_NAME, "main")

        val projectSessionProvider = FirProjectSessionProvider()
        val binaryModuleData = BinaryModuleData.initialize(
            Name.identifier(rootModuleName),
            CommonPlatforms.defaultCommonPlatform,
        )
        val dependencyList = DependencyListForCliModule.build(binaryModuleData)
        val projectEnvironment = VfsBasedProjectEnvironment(
            project,
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            environment::createPackagePartProvider
        )
        val librariesScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

        FirJvmSessionFactory.createLibrarySession(
            Name.identifier(rootModuleName),
            projectSessionProvider,
            dependencyList.moduleDataProvider,
            projectEnvironment,
            FirExtensionRegistrar.getInstances(project),
            librariesScope,
            projectEnvironment.getPackagePartProvider(librariesScope),
            configuration.languageVersionSettings,
            predefinedJavaComponents = null,
            registerExtraComponents = {}
        )

        val commonModuleData = FirModuleDataImpl(
            Name.identifier("$rootModuleName-common"),
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendsDependencies,
            CommonPlatforms.defaultCommonPlatform,
        )

        val platformModuleData = FirModuleDataImpl(
            Name.identifier(rootModuleName),
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies + commonModuleData,
            dependencyList.friendsDependencies,
            JvmPlatforms.jvm8,
        )

        val commonSession = createSourceSession(
            commonModuleData,
            projectSessionProvider,
            projectEnvironment
        )
        val platformSession = createSourceSession(
            platformModuleData,
            projectSessionProvider,
            projectEnvironment
        )

        val commonKtFiles = commonFiles.map { it.toKtFile(project) }
        val platformKtFiles = platformFiles.map { it.toKtFile(project) }

        val reporter = DiagnosticReporterFactory.createReporter()
        val languageVersionSettings = configuration.languageVersionSettings
        val metadataVarsion = configuration.metadataVersion(languageVersionSettings.languageVersion)

        val commonAnalysis =
            buildResolveAndCheckFirFromKtFiles(commonSession, commonKtFiles, metadataVarsion, languageVersionSettings, reporter)
        val platformAnalysis =
            buildResolveAndCheckFirFromKtFiles(platformSession, platformKtFiles, metadataVarsion, languageVersionSettings, reporter)

        return FirAnalysisResult(
            FirResult(listOf(commonAnalysis, platformAnalysis)),
            commonKtFiles + platformKtFiles,
            reporter
        )
    }

    private fun frontend(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): FirFrontendResult {
        val analysisResult = analyze(platformFiles, commonFiles)

        FirDiagnosticsCompilerResultsReporter.throwFirstErrorAsException(
            analysisResult.reporter,
            MessageRenderer.PLAIN_FULL_PATHS
        )

        val fir2IrExtensions = JvmFir2IrExtensions(
            configuration,
            JvmIrDeserializerImpl(),
        )

        val fir2IrResult = analysisResult.firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            analysisResult.reporter,
            IrGenerationExtension.getInstances(project),
        )

        return FirFrontendResult(fir2IrResult, fir2IrExtensions)
    }

    override fun compileToIr(files: List<SourceFile>): IrModuleFragment =
        frontend(files, listOf()).firResult.irModuleFragment

    override fun compile(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): GenerationState {
        val frontendResult = frontend(platformFiles, commonFiles)
        val irModuleFragment = frontendResult.firResult.irModuleFragment
        val components = frontendResult.firResult.components

        val generationState = GenerationState.Builder(
            project,
            ClassBuilderFactories.TEST,
            irModuleFragment.descriptor,
            NoScopeRecordCliBindingTrace(project).bindingContext,
            configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).build()

        generationState.beforeCompile()
        val codegenFactory = JvmIrCodegenFactory(
            configuration,
            configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        )
        codegenFactory.generateModuleInFrontendIRMode(
            generationState,
            irModuleFragment,
            frontendResult.firResult.symbolTable,
            components.irProviders,
            frontendResult.generatorExtensions,
            FirJvmBackendExtension(
                components,
                frontendResult
                    .firResult
                    .irActualizedResult
                    ?.actualizedExpectDeclarations
                    ?.extractFirDeclarations()
            ),
            frontendResult.firResult.pluginContext
        ) {}
        generationState.factory.done()
        return generationState
    }
}
