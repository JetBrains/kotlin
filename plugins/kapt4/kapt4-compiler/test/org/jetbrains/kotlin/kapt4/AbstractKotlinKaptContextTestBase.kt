/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.kapt3.test.*
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

open class AbstractKotlinKapt4ContextTest : AbstractKotlinKapt4ContextTestBase(TargetBackend.JVM_IR)

abstract class AbstractKotlinKapt4ContextTestBase(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    override fun TestConfigurationBuilder.configuration() {
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

        facadeStep(::Kapt4Facade)

        handlersStep(Kapt4ContextBinaryArtifact.Kind) {
            useHandlers(::Kapt4Handler)
        }

        useAfterAnalysisCheckers(::TemporaryKapt4Suppressor)
    }
}

@TestMetadata("plugins/kapt4/kapt4-compiler/testData/converter")
@TestDataPath("\$PROJECT_ROOT")
@Tag("IgnoreJDK11")
class KotlinKapt4ContextTestManual : AbstractKotlinKapt4ContextTest() {
    @Test
    @TestMetadata("simple.kt")
    fun testSimple() {
        runTest("plugins/kapt4/kapt4-compiler/testData/converter/simple.kt")
    }
}
