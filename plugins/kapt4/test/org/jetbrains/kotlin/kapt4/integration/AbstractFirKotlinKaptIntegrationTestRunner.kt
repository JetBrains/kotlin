/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4.integration

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.test.JvmCompilerWithKaptFacade
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptEnvironmentConfigurator
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class AbstractFirKotlinKaptIntegrationTestRunner(
    private val processorOptions: Map<String, String>,
    private val supportedAnnotations: List<String>,
    private val additionalFirPluginExtension: ((FirSession) -> FirFunctionTypeKindExtension)?,
    private val additionalPluginExtension: IrGenerationExtension?,
    private val process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, FirKaptExtensionForTests) -> Unit
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    init {
        doOpenInternalPackagesIfRequired()
    }

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::KaptEnvironmentConfigurator.bind(processorOptions),
            { FirKaptExtensionRegistrarConfigurator(it, additionalFirPluginExtension) },
            { FirKaptIntegrationEnvironmentConfigurator(it, processorOptions, supportedAnnotations, process) }
        )

        facadeStep { services -> JvmCompilerWithKaptFacade(services, additionalPluginExtension) }
        handlersStep(KaptContextBinaryArtifact.Kind) {
            useHandlers(::FirKaptIntegrationStubsDumpHandler, ::FirProcessorWasCalledHandler)
        }

        useAdditionalService(::FirKaptExtensionProvider)
    }
}

class FirKaptExtensionRegistrarConfigurator(
    testServices: TestServices,
    private val additionalFirPluginExtension: ((FirSession) -> FirFunctionTypeKindExtension)?,
): EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        if (additionalFirPluginExtension != null) {
            FirExtensionRegistrarAdapter.registerExtension(FirKaptExtensionRegistrar(additionalFirPluginExtension))
        }
    }
}

class FirKaptExtensionRegistrar(
    private val additionalFirPluginExtension: (FirSession) -> FirFunctionTypeKindExtension,
): FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +additionalFirPluginExtension
    }
}