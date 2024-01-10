/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.runners

import org.jetbrains.kotlin.js.test.fir.AbstractFirJsTest
import org.jetbrains.kotlin.js.test.ir.AbstractJsIrTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

open class AbstractFirJsObjectIrJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "plugins/kotlinx-serialization/testData/boxIr/",
    testGroupOutputDirPrefix = "codegen/serializationBoxIr/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxJsObject()
    }
}