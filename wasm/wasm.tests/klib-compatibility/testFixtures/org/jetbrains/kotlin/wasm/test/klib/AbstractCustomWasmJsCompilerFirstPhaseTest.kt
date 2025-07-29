/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerFirstPhaseEnvironmentConfigurator
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerFirstPhaseFacade
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.wasmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstPhaseTestSuppressor
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmBoxTest
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunner
import org.junit.jupiter.api.Tag

@Tag("custom-first-phase")
open class AbstractCustomWasmJsCompilerFirstPhaseTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.WASM) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            // Note: Need to specify the concrete FE kind because this affects the choice of IGNORE_BACKEND_* directive.
            frontend = if (customWasmJsCompilerSettings.defaultLanguageVersion.usesK2) FrontendKinds.FIR else FrontendKinds.ClassicFrontend
            targetPlatform = WasmPlatforms.wasmJs
            dependencyKind = DependencyKind.Binary
        }

        commonConfigurationForWasmBoxTest(
            pathToTestDir = "compiler/testData/codegen/box/",
            testGroupOutputDirPrefix = "customWasmJsCompilerFirstPhaseTest/",
            wasmEnvironmentConfigurator = ::WasmEnvironmentConfiguratorJs,
            additionalSourceProvider = null,
            customIgnoreDirective = null,
            additionalIgnoreDirectives = null,
        )

        defaultDirectives {
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }

        useConfigurators(
            // And this configurator is necessary for the first (custom) compilation phase:
            ::CustomWebCompilerFirstPhaseEnvironmentConfigurator,
        )

        facadeStep(::CustomWebCompilerFirstPhaseFacade)

        // Suppress all tests that have not been successfully compiled by the first phase.
        useAfterAnalysisCheckers(
            ::CustomKlibCompilerFirstPhaseTestSuppressor,
            insertAtFirst = true,
        )

        facadeStep(::WasmBackendFacade)

        wasmArtifactsHandlersStep {
            useHandlers(::WasmBoxRunner)
        }
    }
}
