/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.fir.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsTest
import org.jetbrains.kotlin.js.test.ir.configureJsBoxHandlers
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag

@Tag("custom-second-phase")
open class AbstractCustomJsCompilerSecondPhaseTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // Don't run this test if KLIBs produced by the first phase are not consumable by the older compiler versions
        // used on the second phase.
        Assumptions.assumeTrue(customJsCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
        // KT-47200: TODO export `box()` by means of Gradle configuration, and not using `JsExportBoxPreprocessor` hack.
        useSourcePreprocessor(::JsExportBoxPreprocessor)
        defaultDirectives {
            // `js-ir-minimal-for-test` must not be used in this test at all, so need to use `kotlin-test` library via `WITH_STDLIB` directive
            // Note: attempt to use `js-ir-minimal-for-test` on 1st stage will cause unresolved symbol `assertEquals(0:0;0:0){0§<kotlin.Any?>}`
            // on 2nd stage, since this symbol is absent in `kotlin-test` library.
            +WITH_STDLIB
        }

        setUpDefaultDirectivesForJsBoxTest(FirParser.LightTree)

        commonConfigurationForJsTest()

        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::CustomJsCompilerSecondPhaseFacade)

        jsArtifactsHandlersStep()
        configureJsBoxHandlers()

        useAfterAnalysisCheckers(
            // Suppress all tests that failed on the first phase if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
        )
        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

// Makes `box()` exported during CLI invocation of the previous compiler, so it can be invoked by the test runner.
// In the pure test pipeline the same is done in `JsIrLoweringFacade.compileIrToJs()` by passing `exportedDeclarations` param to `jsCompileKt.compileIr()`
class JsExportBoxPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private val topLevelBoxRegex = Regex("(^|\n)fun box\\(\\)")
    private val topLevelBoxReplacement = "\n@JsExport fun box()"

    override fun process(file: TestFile, content: String): String {
        return topLevelBoxRegex.replace(content, topLevelBoxReplacement)
    }
}
