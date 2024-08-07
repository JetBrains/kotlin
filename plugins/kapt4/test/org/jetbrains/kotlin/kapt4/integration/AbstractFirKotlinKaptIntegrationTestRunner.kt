/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4.integration

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptEnvironmentConfigurator
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives
import org.jetbrains.kotlin.kapt4.FirJvmCompilerWithKaptFacade
import org.jetbrains.kotlin.kapt4.FirKaptContextBinaryArtifact
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class AbstractFirKotlinKaptIntegrationTestRunner(
    private val processorOptions: Map<String, String>,
    private val supportedAnnotations: List<String>,
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
            { FirKaptIntegrationEnvironmentConfigurator(it, processorOptions, supportedAnnotations, process) }
        )

        facadeStep { services -> FirJvmCompilerWithKaptFacade(services, additionalPluginExtension) }
        handlersStep(FirKaptContextBinaryArtifact.Kind) {
            useHandlers(::FirKaptIntegrationStubsDumpHandler, ::FirProcessorWasCalledHandler)
        }

        useAdditionalService(::FirKaptExtensionProvider)
    }
}
