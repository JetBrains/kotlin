/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.integration

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.kapt3.base.KaptFlag
import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.test.JvmCompilerWithKaptFacade
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptEnvironmentConfigurator
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class AbstractKotlinKapt3IntegrationTestRunner(
    targetBackend: TargetBackend,
    private val processorOptions: Map<String, String>,
    private val supportedAnnotations: List<String>,
    private val additionalPluginExtension: IrGenerationExtension?,
    private val process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, Kapt3ExtensionForTests) -> Unit
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {

    init {
        doOpenInternalPackagesIfRequired()
    }

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
            if (!targetBackend.isIR) {
                KaptTestDirectives.DISABLED_FLAGS with KaptFlag.USE_JVM_IR
            }
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::KaptEnvironmentConfigurator.bind(processorOptions),
            { KaptIntegrationEnvironmentConfigurator(it, processorOptions, supportedAnnotations, process) }
        )

        facadeStep { services -> JvmCompilerWithKaptFacade(services, additionalPluginExtension) }
        handlersStep(KaptContextBinaryArtifact.Kind) {
            useHandlers(::KaptIntegrationStubsDumpHandler, ::ProcessorWasCalledHandler)
        }

        useAdditionalService(::Kapt3ExtensionProvider)
    }
}
