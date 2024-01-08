/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.js.test.fir.AbstractFirJsTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization
import org.jetbrains.kotlin.js.test.ir.AbstractJsIrTest;
import org.jetbrains.kotlin.test.TargetBackend

open class AbstractSerializationIrBoxTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization()
    }
}

open class AbstractSerializationJdk11IrBoxTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(useJdk11 = true)
    }
}

open class AbstractSerializationWithoutRuntimeIrBoxTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(noLibraries = true)
    }
}

open class AbstractSerializationFirLightTreeBlackBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization()
    }
}

open class AbstractSerializationJdk11FirLightTreeBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(useJdk11 = true)
    }
}

open class AbstractSerializationWithoutRuntimeFirLightTreeBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(noLibraries = true)
    }
}

open class AbstractSerializationIrJsBoxTest : AbstractJsIrTest(
    pathToTestDir = "plugins/kotlinx-serialization/testData/boxIr/",
    testGroupOutputDirPrefix = "codegen/serializationBoxIr/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(target = TargetBackend.JS_IR)
    }
}

open class AbstractSerializationFirJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "plugins/kotlinx-serialization/testData/boxIr/",
    testGroupOutputDirPrefix = "codegen/serializationBoxFir/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(target = TargetBackend.JS_IR)
    }
}
