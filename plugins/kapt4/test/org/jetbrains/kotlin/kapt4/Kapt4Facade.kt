/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
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

internal class Kapt4Facade(private val testServices: TestServices) :
    AbstractTestFacade<ResultingArtifact.Source, KaptContextBinaryArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<KaptContextBinaryArtifact>
        get() = KaptContextBinaryArtifact.Kind

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::KaptMessageCollectorProvider))

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): KaptContextBinaryArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider

        val configuration = configurationProvider.getCompilerConfiguration(module)

        val messageCollector = testServices.messageCollectorProvider.getCollector(module)

        val ktFiles = testServices.sourceFileProvider.getKtSourceFilesForSourceFiles(module.files).values.toList()

        val groupedSources = GroupedKtSources(ktFiles, emptyList(), emptyMap())

        val compilerInput = ModuleCompilerInput(
            TargetId(module.name, "test"),
            groupedSources,
            CommonPlatforms.defaultCommonPlatform,
            JvmPlatforms.unspecifiedJvmPlatform,
            configuration
        )

        val projectDisposable = Disposer.newDisposable("K2KaptSession.project")
        val projectEnvironment =
            createProjectEnvironment(configuration, projectDisposable, EnvironmentConfigFiles.JVM_CONFIG_FILES, messageCollector)

        val diagnosticsReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)

        val analysisResults = compileModuleToAnalyzedFir(
            compilerInput,
            projectEnvironment,
            emptyList(),
            null,
            diagnosticsReporter,
        )

        val cleanDiagnosticReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)
        val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, cleanDiagnosticReporter)
        val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment, skipBodies = true)
        val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment, skipBodies = true)

        val builderFactory = codegenOutput.builderFactory
        val compiledClasses = (builderFactory as OriginCollectingClassBuilderFactory).compiledClasses
        val origins = builderFactory.origins

        val options = testServices.kaptOptionsProvider[module]

        val logger = MessageCollectorBackedKaptLogger(
            isVerbose = true,
            isInfoAsWarnings = false,
            messageCollector = testServices.messageCollectorProvider.getCollector(module)
        )

        val context = KaptContextForStubGeneration(
            options, true, logger, compiledClasses, origins, codegenOutput.generationState,
            analysisResults.outputs.flatMap { it.fir }
        )
        return KaptContextBinaryArtifact(context, isFir = true)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}

