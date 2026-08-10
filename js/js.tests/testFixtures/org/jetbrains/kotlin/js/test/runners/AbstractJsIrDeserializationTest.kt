/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.IGNORE_IR_DESERIALIZATION_TEST
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor

/**
 * Base class for IR deserialization tests, configured with FIR frontend.
 */
abstract class AbstractJsIrDeserializationTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractJsBlackBoxCodegenTestBase(TargetBackend.JS_IR, pathToTestDir, testGroupOutputDirPrefix) {
    override val backendFacades: JsBackendFacades
        get() = JsBackendFacades.WithSeparatedDeserialization

    override val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = IGNORE_IR_DESERIALIZATION_TEST

    override val enableBoxHandlers: Boolean
        get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
            }
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor
            )
        }
    }
}

abstract class AbstractJsIrDeserializationCodegenBoxTest : AbstractJsIrDeserializationTest(
    pathToTestDir = "compiler/testData/codegen/",
    testGroupOutputDirPrefix = "irDeserialization/codegenBox/",
)
