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
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirViaLightTree
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name

class FirAnalysisResult(
    val frontendOutput: AllModulesFrontendOutput,
    val reporter: BaseDiagnosticsCollector,
)

class K2CompilerFacade(environment: KotlinCoreEnvironment) : KotlinCompilerFacade(environment) {
    private val project: Project
        get() = environment.project

    private val configuration: CompilerConfiguration
        get() = environment.configuration

    override fun analyze(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>,
    ): FirAnalysisResult {
        val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME, "main")
        val projectEnvironment = VfsBasedProjectEnvironment(
            project,
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            environment::createPackagePartProvider
        )

        val commonKtFiles = commonFiles.map { it.toKtSourceFile() }
        val commonKtFileSet = commonKtFiles.toSet()
        val sessionsWithSources = JvmFrontendPipelinePhase.prepareJvmSessions(
            files = commonKtFiles + platformFiles.map { it.toKtSourceFile() },
            rootModuleName = Name.special("<$moduleName>"),
            configuration = configuration,
            projectEnvironment = projectEnvironment,
            librariesClasspath = JvmClasspath.ProjectLibraries(),
            libraryList = JvmFrontendPipelinePhase.createLibraryListForJvm(moduleName, configuration, friendPaths = emptyList()),
            isCommonSource = { it in commonKtFileSet },
            isScript = { false },
            fileBelongsToModule = { _, _ -> false },
            incrementalCompilationContext = null,
        )

        val reporter = DiagnosticsCollectorImpl()
        val outputs = sessionsWithSources.map { (val session, val files) ->
            buildResolveAndCheckFirViaLightTree(session, files, reporter, countFilesAndLines = null)
        }

        return FirAnalysisResult(AllModulesFrontendOutput(outputs), reporter)
    }

    private fun frontend(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>,
    ): Fir2IrActualizedResult {
        val analysisResult = analyze(platformFiles, commonFiles)

        FirDiagnosticsCompilerResultsReporter.throwFirstErrorAsException(
            analysisResult.reporter,
            MessageRenderer.PLAIN_FULL_PATHS
        )

        val fir2IrExtensions = JvmFir2IrExtensions(
        )

        return analysisResult.frontendOutput.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            analysisResult.reporter,
            configuration.getCompilerExtensions(IrGenerationExtension),
        )
    }

    override fun compileToIr(files: List<SourceFile>): IrModuleFragment =
        frontend(files, listOf()).irModuleFragment

    override fun compile(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>,
    ): GenerationState {
        val frontendResult = frontend(platformFiles, commonFiles)
        val irModuleFragment = frontendResult.irModuleFragment
        val components = frontendResult.components

        val generationState = GenerationState(
            project,
            irModuleFragment.descriptor,
            configuration,
            ClassBuilderFactories.TEST,
            jvmBackendClassResolver = FirJvmBackendClassResolver(components),
        )

        val backendInput = JvmIrCodegenFactory.BackendInput(
            irModuleFragment,
            frontendResult.pluginContext.irBuiltIns,
            frontendResult.symbolTable,
            debuggerExtensions = null,
            FirJvmBackendExtension(
                components,
                frontendResult
                    .irActualizedResult
                    ?.actualizedExpectDeclarations
                    ?.extractFirDeclarations()
            ),
            frontendResult.pluginContext,
        )
        JvmIrCodegenFactory(configuration).generateModule(generationState, backendInput)
        return generationState
    }
}
