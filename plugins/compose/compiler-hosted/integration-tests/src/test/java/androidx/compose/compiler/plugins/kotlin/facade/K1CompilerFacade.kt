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

import androidx.compose.compiler.plugins.kotlin.TestsCompilerError
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

class K1AnalysisResult(
    override val files: List<KtFile>,
    val moduleDescriptor: ModuleDescriptor,
    val bindingContext: BindingContext
) : AnalysisResult {
    override val diagnostics: Map<String, List<AnalysisResult.Diagnostic>>
        get() = bindingContext.diagnostics.all().groupBy(
            keySelector = { it.psiFile.name },
            valueTransform = { AnalysisResult.Diagnostic(it.factoryName, it.textRanges) }
        )
}

private class K1FrontendResult(
    val state: GenerationState,
    val backendInput: JvmIrCodegenFactory.JvmIrBackendInput,
    val codegenFactory: JvmIrCodegenFactory
)

class K1CompilerFacade(environment: KotlinCoreEnvironment) : KotlinCompilerFacade(environment) {
    override fun analyze(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): K1AnalysisResult {
        val allKtFiles = platformFiles.map { it.toKtFile(environment.project) } +
            commonFiles.map {
                it.toKtFile(environment.project).also { ktFile ->
                    ktFile.isCommonSource = true
                }
            }

        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            environment.project,
            allKtFiles,
            CliBindingTrace(),
            environment.configuration,
            environment::createPackagePartProvider
        )

        try {
            result.throwIfError()
        } catch (e: Exception) {
            throw TestsCompilerError(e)
        }

        return K1AnalysisResult(allKtFiles, result.moduleDescriptor, result.bindingContext)
    }

    private fun frontend(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): K1FrontendResult {
        val analysisResult = analyze(platformFiles, commonFiles)

        // `analyze` only throws if the analysis itself failed, since we use it to test code
        // with errors. That's why we have to check for errors before we run psi2ir.
        try {
            AnalyzingUtils.throwExceptionOnErrors(analysisResult.bindingContext)
        } catch (e: Exception) {
            throw TestsCompilerError(e)
        }

        val codegenFactory = JvmIrCodegenFactory(
            environment.configuration,
            environment.configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        )

        val state = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.TEST,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            analysisResult.files,
            environment.configuration
        ).isIrBackend(true).codegenFactory(codegenFactory).build()

        state.beforeCompile()

        val psi2irInput = CodegenFactory.IrConversionInput.fromGenerationStateAndFiles(
            state,
            analysisResult.files
        )
        val backendInput = codegenFactory.convertToIr(psi2irInput)

        // For JVM-specific errors
        try {
            AnalyzingUtils.throwExceptionOnErrors(state.collectedExtraJvmDiagnostics)
        } catch (e: Throwable) {
            throw TestsCompilerError(e)
        }

        return K1FrontendResult(
            state,
            backendInput,
            codegenFactory
        )
    }

    override fun compileToIr(files: List<SourceFile>): IrModuleFragment =
        frontend(files, listOf()).backendInput.irModuleFragment

    override fun compile(
        platformFiles: List<SourceFile>,
        commonFiles: List<SourceFile>
    ): GenerationState = try {
        frontend(platformFiles, commonFiles).apply {
            codegenFactory.generateModule(state, backendInput)
            state.factory.done()
        }.state
    } catch (e: Exception) {
        throw TestsCompilerError(e)
    }
}
