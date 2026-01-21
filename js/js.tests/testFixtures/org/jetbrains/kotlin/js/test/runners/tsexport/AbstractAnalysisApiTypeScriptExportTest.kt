/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners.tsexport

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.AnalysisApiBasedDtsGeneratorFacade
import org.jetbrains.kotlin.js.test.runners.commonConfigurationForJsBackendFirstStageTest
import org.jetbrains.kotlin.js.test.runners.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest

/**
 * Unlike [AbstractJsTypeScriptExportTest], which runs the full compiler pipeline and also executes the generated code in addition
 * to verifying the generated `.d.ts` files, in these tests we only run the first compilation stage (which produces KLIBs),
 * generate `.d.ts` files from Kotlin metadata serialized in those KLIBs, and verify them without running the generated code.
 */
abstract class AbstractAnalysisApiTypeScriptExportTest : AbstractKotlinCompilerTest() {
    protected fun TestConfigurationBuilder.configureTypeScriptExport() {
        defaultDirectives {
            LANGUAGE with listOf(
                "-${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
            DIAGNOSTICS with "-warnings"
        }

        facadeStep(::AnalysisApiBasedDtsGeneratorFacade)
        jsArtifactsHandlersStep()

        // TODO(KT-82224): Add a separate test runner with isWholeFileJsExport = true when we implement support for file-level @JsExport
        //  in Analysis API-based TypeScript Export
        configureJsTypeScriptExportTest(isWholeFileJsExport = false, expectedDtsSuffix = "aa")
    }
}

abstract class AbstractJsAnalysisApiTypeScriptExportTest : AbstractAnalysisApiTypeScriptExportTest() {
    open val targetBackend: TargetBackend
        get() = TargetBackend.JS_IR

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            targetBackend = this@AbstractJsAnalysisApiTypeScriptExportTest.targetBackend
        }
        setUpDefaultDirectivesForJsBoxTest(FirParser.LightTree)
        commonConfigurationForJsBackendFirstStageTest(
            customIgnoreDirective = IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT,
        )
        configureTypeScriptExport()
    }
}

abstract class AbstractJsES6AnalysisApiTypeScriptExportTest : AbstractJsAnalysisApiTypeScriptExportTest() {
    override val targetBackend: TargetBackend
        get() = TargetBackend.JS_IR_ES6

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            +JsEnvironmentConfigurationDirectives.ES6_MODE
        }
    }
}