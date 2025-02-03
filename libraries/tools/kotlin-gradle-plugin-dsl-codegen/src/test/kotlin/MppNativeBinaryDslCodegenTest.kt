/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import org.jetbrains.kotlin.generators.arguments.getPrinterForTests
import kotlin.test.Ignore
import kotlin.test.Test

class MppNativeBinaryDslCodegenTest {
    @Test
    @Ignore // TODO: fix in follow-up commits
    fun testBinaryContainerCodegenIsUpToDate() {
        generateAbstractKotlinNativeBinaryContainer(::getPrinterForTests)
    }
}