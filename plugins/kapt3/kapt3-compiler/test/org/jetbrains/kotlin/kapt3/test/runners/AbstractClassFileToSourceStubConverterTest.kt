/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.runners

import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.test.JvmCompilerWithKaptFacade
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptEnvironmentConfigurator
import org.jetbrains.kotlin.kapt3.test.KaptRegularExtensionForTestConfigurator
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.USE_JVM_IR
import org.jetbrains.kotlin.kapt3.test.handlers.ClassFileToSourceKaptStubHandler
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractClassFileToSourceStubConverterTestBase(
    targetBackend: TargetBackend
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
            +MAP_DIAGNOSTIC_LOCATIONS
            +WITH_STDLIB
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::KaptEnvironmentConfigurator,
            ::KaptRegularExtensionForTestConfigurator,
        )

        facadeStep(::JvmCompilerWithKaptFacade)
        handlersStep(KaptContextBinaryArtifact.Kind) {
            useHandlers(::ClassFileToSourceKaptStubHandler)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

open class AbstractClassFileToSourceStubConverterTest : AbstractClassFileToSourceStubConverterTestBase(TargetBackend.JVM) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            globalDefaults {
                targetBackend = TargetBackend.JVM
            }
        }
    }
}

open class AbstractIrClassFileToSourceStubConverterTest : AbstractClassFileToSourceStubConverterTestBase(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            globalDefaults {
                targetBackend = TargetBackend.JVM_IR
            }
            defaultDirectives {
                +USE_JVM_IR
            }
        }
    }
}
