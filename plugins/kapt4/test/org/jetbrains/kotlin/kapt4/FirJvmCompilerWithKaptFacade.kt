/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.runFrontendForAnalysis
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.kapt3.test.messageCollectorProvider
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class FirJvmCompilerWithKaptFacade(
    private val testServices: TestServices,
) :
    AbstractTestFacade<ResultingArtifact.Source, KaptContextBinaryArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<KaptContextBinaryArtifact>
        get() = KaptContextBinaryArtifact.Kind

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::KaptMessageCollectorProvider))

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): KaptContextBinaryArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider

        val configuration = configurationProvider.getCompilerConfiguration(module).copy().apply {
            put(JVMConfigurationKeys.SKIP_BODIES, true)
        }

        val messageCollector = testServices.messageCollectorProvider.getCollector(module)

        val ktSourceFiles = testServices.sourceFileProvider.getKtSourceFilesForSourceFiles(module.files).values.toList()

        val groupedSources = GroupedKtSources(ktSourceFiles, emptyList(), emptyMap())

        val projectEnvironment = createProjectEnvironment(
            configuration, testServices.compilerConfigurationProvider.testRootDisposable, EnvironmentConfigFiles.JVM_CONFIG_FILES,
            messageCollector,
        )

        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(
            module.files, projectEnvironment.project, findViaVfs = true,
        ).values.toList()
        val cliModule = ModuleBuilder(module.name, "", "test")
        val analysisResults = runFrontendForAnalysis(
            projectEnvironment,
            configurationProvider.getCompilerConfiguration(module),
            messageCollector,
            ktFiles,
            null,
            cliModule,
        )

        val cleanDiagnosticReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)
        val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, cleanDiagnosticReporter)
        val compilerInput = ModuleCompilerInput(
            TargetId(cliModule),
            groupedSources,
            CommonPlatforms.defaultCommonPlatform,
            JvmPlatforms.unspecifiedJvmPlatform,
            configuration,
        )
        val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment)
        val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment)

        val classBuilderFactory = codegenOutput.builderFactory as OriginCollectingClassBuilderFactory

        val logger = MessageCollectorBackedKaptLogger(
            isVerbose = true,
            isInfoAsWarnings = false,
            messageCollector = testServices.messageCollectorProvider.getCollector(module)
        )

        val kaptContext = KaptContextForStubGeneration(
            testServices.kaptOptionsProvider[module],
            withJdk = true,
            logger,
            classBuilderFactory.compiledClasses,
            classBuilderFactory.origins,
            codegenOutput.generationState,
            analysisResults.outputs.flatMap { it.fir },
        )
        return KaptContextBinaryArtifact(kaptContext)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}
