/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners.tsexport

import org.jetbrains.kotlin.js.test.converters.AnalysisApiBasedDtsGenerationHandler
import org.jetbrains.kotlin.js.test.runners.AbstractJsES6Test
import org.jetbrains.kotlin.js.test.runners.AbstractJsTest
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.testFederation.AffectedByAnalysisApi

@AffectedByAnalysisApi
abstract class AbstractJsAnalysisApiTypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export-aa/es5",
    private val isWholeFileJsExport: Boolean = false,
) : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/js/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
    parser = FirParser.LightTree,
) {
    override val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsAnalysisApiTypeScriptExportTest(isWholeFileJsExport)
    }
}

abstract class AbstractJsAnalysisApiTypeScriptWholeFileExportTest : AbstractJsAnalysisApiTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export-aa/es5-whole-file",
    isWholeFileJsExport = true,
)

@AffectedByAnalysisApi
abstract class AbstractJsES6AnalysisApiTypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export-aa/es6",
    private val isWholeFileJsExport: Boolean = false
) : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/js/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
    parser = FirParser.LightTree,
) {
    override val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        builder.configureJsAnalysisApiTypeScriptExportTest(isWholeFileJsExport)
    }
}

abstract class AbstractJsES6AnalysisApiTypeScriptWholeFileExportTest : AbstractJsES6AnalysisApiTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export-aa/es6-whole-file",
    isWholeFileJsExport = true,
)

private fun TestConfigurationBuilder.configureJsAnalysisApiTypeScriptExportTest(isWholeFileJsExport: Boolean) {
    defaultDirectives {
        DIAGNOSTICS with "-warnings"
    }
    configureKlibArtifactsHandlersStep {
        useHandlers(::AnalysisApiBasedDtsGenerationHandler)
    }
    configureJsTypeScriptExportTest(isWholeFileJsExport, expectedDtsSuffix = "aa")
}
