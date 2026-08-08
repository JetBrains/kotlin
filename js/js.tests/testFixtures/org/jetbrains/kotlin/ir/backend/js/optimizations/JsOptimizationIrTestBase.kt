/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations

import org.jetbrains.kotlin.js.test.converters.*
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator

/**
 * Shared pipeline: FIR -> klib -> deserialize -> full JS IR lowering via [JsIrOptimizationLoweringFacade].
 * Used by JS IR dataflow / optimization tests that need late lowered IR.
 */
abstract class JsOptimizationIrTestBase(
    private val handler: (TestServices) -> AbstractIrHandler,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JsPlatforms.defaultJsPlatform
            targetBackend = TargetBackend.JS_IR
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        useAdditionalService(::LibraryProvider)

        facadeStep(::FirCliWebFacade)
        firHandlersStep()
        facadeStep(::Fir2IrCliWebFacade)
        irHandlersStep()
        facadeStep(::JsIrPreSerializationLoweringFacade)
        loweredIrHandlersStep()
        facadeStep(::FirKlibSerializerCliJsFacade)
        facadeStep(::JsIrDeserializerFacade)
        facadeStep(::JsIrOptimizationLoweringFacade)
        deserializedIrHandlersStep {
            useHandlers(handler)
        }

        configureFirParser(FirParser.LightTree)

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsFirstStageEnvironmentConfigurator,
            ::JsSecondStageEnvironmentConfigurator,
        )
    }
}
