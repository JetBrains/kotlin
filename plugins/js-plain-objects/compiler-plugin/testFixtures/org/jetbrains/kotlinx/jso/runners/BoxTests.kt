/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.runners

import org.jetbrains.kotlin.js.test.runners.AbstractJsTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractJsPlainObjectsBoxTest : AbstractJsTest(
    pathToTestDir = "plugins/js-plain-objects/compiler-plugin/testData/box",
    testGroupOutputDirPrefix = "jsPlainObjectsBox/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxJsPlainObjects()
    }
}