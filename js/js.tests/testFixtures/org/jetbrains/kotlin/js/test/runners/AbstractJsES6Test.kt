/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsES6Test(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    parser: FirParser = FirParser.Psi
) : AbstractJsTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6, parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.ES6_MODE
            }
        }
        builder.configureLoweredIrDumpHandlers()
    }
}


abstract class AbstractJsES6BoxTest(
    testGroupOutputDirPrefix: String = "es6Box/",
) : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
)

abstract class AbstractJsES6BoxWithInlineAnonymousFunctionsTest : AbstractJsES6BoxTest(
    testGroupOutputDirPrefix = "es6BoxWithInlineAnonymousFunctions/"
) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>
        get() = listOf(IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +GENERATE_INLINE_ANONYMOUS_FUNCTIONS
        }
    }
}

abstract class AbstractJsES6CodegenBoxTest(
    testGroupOutputDirPrefix: String = "codegen/es6Box/"
) : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useFailureSuppressors(
            ::FirMetaInfoDiffSuppressor
        )

        builder.configureIrHandlersStep {
            commonIrHandlersForCodegenTest()
        }
    }
}

abstract class AbstractJsES6CodegenBoxWithInlineAnonymousFunctionsTest : AbstractJsES6CodegenBoxTest(
    testGroupOutputDirPrefix = "codegen/es6BoxWithInlineAnonymousFunctions/",
) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>
        get() = listOf(IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +GENERATE_INLINE_ANONYMOUS_FUNCTIONS
        }
    }
}

abstract class AbstractJsES6CodegenInlineTest(
    testGroupOutputDirPrefix: String = "codegen/es6BoxInline/",
) : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
)

abstract class AbstractJsES6CodegenBoxInlineWithInlineAnonymousFunctionsTest : AbstractJsES6CodegenInlineTest(
    testGroupOutputDirPrefix = "codegen/es6BoxInlineWithInlineAnonymousFunctions/",
) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>
        get() = listOf(IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +GENERATE_INLINE_ANONYMOUS_FUNCTIONS
        }
    }
}

abstract class AbstractJsES6CodegenWasmJsInteropTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxWasmJsInterop",
    testGroupOutputDirPrefix = "codegen/boxWasmJsInteropEs6",
)
