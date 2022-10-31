/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsIrES6Test(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractJsIrTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.ES6_MODE
            }
        }
    }
}

open class AbstractIrBoxJsES6Test : AbstractJsIrES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "irEs6Box/"
)

open class AbstractIrJsES6CodegenBoxTest : AbstractJsIrES6Test(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/irEs6Box/"
)

open class AbstractIrJsES6CodegenBoxErrorTest : AbstractJsIrES6Test(
    pathToTestDir = "compiler/testData/codegen/boxError/",
    testGroupOutputDirPrefix = "codegen/irEs6BoxError/"
)

open class AbstractIrJsES6CodegenInlineTest : AbstractJsIrES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/irEs6BoxInline/"
)