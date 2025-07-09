/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.JsFailingTestSuppressor
import org.jetbrains.kotlin.js.test.fir.setupDefaultDirectivesForFirJsBoxTest
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsBackendSecondStageTest
import org.jetbrains.kotlin.js.test.ir.configureJsBoxHandlers
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

open class AbstractCustomJsCompilerFirstPhaseTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = /* Does not matter */ FrontendKinds.FIR
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::CoroutineHelpersSourceFilesProvider, // TODO: is it really necessary?
            ::AdditionalDiagnosticsSourceFilesProvider, // TODO: is it really necessary?
        )

        facadeStep(::CustomWebCompilerFirstPhaseFacade)

        useAfterAnalysisCheckers(
            // TODO: add a specific suppressor to ignore tests with compilation errors!
            ::JsFailingTestSuppressor, // TODO: is it really necessary?
            ::BlackBoxCodegenSuppressor, // TODO: is it really necessary?
        )

        enableMetaInfoHandler() // // TODO: is it really necessary?

        commonConfigurationForJsBackendSecondStageTest(
            pathToTestDir = "compiler/testData/codegen/box/",
            testGroupOutputDirPrefix = "customJsCompilerFirstPhaseTest/",
            backendFacades = JsBackendFacades.WithRecompilation
        )

        setupDefaultDirectivesForFirJsBoxTest(parser = /* Does not matter */ FirParser.LightTree)

        configureJsBoxHandlers()
    }
}