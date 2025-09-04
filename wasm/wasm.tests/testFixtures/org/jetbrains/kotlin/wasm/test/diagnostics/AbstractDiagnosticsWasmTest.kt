/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.classicFrontendStep
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractDiagnosticsWasmTestBase(
    private val targetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = this@AbstractDiagnosticsWasmTestBase.targetPlatform
            dependencyKind = DependencyKind.Source
            targetBackend = TargetBackend.WASM
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
            LANGUAGE with listOf(
                "-${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }

        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::WasmEnvironmentConfigurator.bind(wasmTarget),
        )

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )
        useAdditionalService(::LibraryProvider)
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        classicFrontendStep()
        classicFrontendHandlersStep {
            useHandlers(
                ::ClassicDiagnosticsHandler,
            )
        }

        forTestsMatching("compiler/testData/diagnostics/wasmTests/multiplatform/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE + "+MultiPlatformProjects"
            }
        }
    }
}

abstract class AbstractDiagnosticsWasmTest : AbstractDiagnosticsWasmTestBase(
    WasmPlatforms.wasmJs,
    WasmTarget.JS,
)

abstract class AbstractDiagnosticsWasmWasiTest : AbstractDiagnosticsWasmTestBase(
    WasmPlatforms.wasmWasi,
    WasmTarget.WASI,
)
