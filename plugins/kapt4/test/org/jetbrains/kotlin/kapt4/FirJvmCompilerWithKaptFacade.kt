/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.kapt3.test.messageCollectorProvider
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class FirJvmCompilerWithKaptFacade(
    private val testServices: TestServices,
    private val additionalPluginExtension: IrGenerationExtension? = null,
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
        val project = configurationProvider.getProject(module)
        if (additionalPluginExtension != null) {
            IrGenerationExtension.registerExtension(project, additionalPluginExtension)
        }
        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project, findViaVfs = true).values.toList()
        val classBuilderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
        val compilerConfiguration = configurationProvider.getCompilerConfiguration(module)
        val moduleBuilder = ModuleBuilder(module.name, "", "test-module")
        compilerConfiguration.put(JVMConfigurationKeys.MODULES, listOf(moduleBuilder)) // Since module is used in FIR
        val generationState = GenerationUtils.compileFiles(
            ktFiles,
            compilerConfiguration,
            classBuilderFactory,
            configurationProvider.getPackagePartProviderFactory(module)
        )
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
            generationState,
            emptyList()
        )
        return KaptContextBinaryArtifact(kaptContext)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}

class FirKaptContextBinaryArtifact(val kaptContext: KaptContextForStubGeneration) : ResultingArtifact.Binary<FirKaptContextBinaryArtifact>() {
    object Kind : BinaryKind<FirKaptContextBinaryArtifact>("FirKaptArtifact")

    override val kind: BinaryKind<FirKaptContextBinaryArtifact>
        get() = Kind
}
