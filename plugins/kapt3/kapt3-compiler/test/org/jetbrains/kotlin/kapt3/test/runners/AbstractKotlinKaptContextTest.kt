/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.runners

import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.test.*
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
import org.jetbrains.kotlin.kapt3.test.handlers.KaptContextHandler
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractKotlinKaptContextTestBase(
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
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::KaptEnvironmentConfigurator,
            ::KaptRegularExtensionForTestConfigurator,
        )

        facadeStep(::JvmCompilerWithKaptFacade)
        handlersStep(KaptContextBinaryArtifact.Kind) {
            useHandlers(::KaptContextHandler)
        }
    }
}

open class AbstractKotlinKaptContextTest : AbstractKotlinKaptContextTestBase(TargetBackend.JVM) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            globalDefaults {
                targetBackend = TargetBackend.JVM
            }
        }
    }
}

open class AbstractIrKotlinKaptContextTest : AbstractKotlinKaptContextTestBase(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            globalDefaults {
                targetBackend = TargetBackend.JVM_IR
            }
            defaultDirectives {
                +KaptTestDirectives.USE_JVM_IR
            }
        }
    }
}
