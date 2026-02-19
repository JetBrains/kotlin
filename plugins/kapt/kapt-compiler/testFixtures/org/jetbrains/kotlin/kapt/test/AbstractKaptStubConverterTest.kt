/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.kapt.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
import org.jetbrains.kotlin.kapt.test.handlers.KaptStubConverterHandler
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

open class AbstractKaptStubConverterTest : AbstractKotlinCompilerTest() {
    init {
        doOpenInternalPackagesIfRequired()
    }

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
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
        )

        facadeStep(::JvmCompilerWithKaptFacade)
        handlersStep(KaptContextBinaryArtifact.Kind, CompilationStage.FIRST) {
            useHandlers(::KaptStubConverterHandler)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}
