/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget

class HierarchicalPropertyCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple property`() {
        val result = commonize {
            outputTarget("((a, b), (c, d))")
            simpleSingleSourceTarget("a", "val x: Int = 42")
            simpleSingleSourceTarget("b", "val x: Int = 42")
            simpleSingleSourceTarget("c", "val x: Int = 42")
            simpleSingleSourceTarget("d", "val x: Int = 42")
        }

        result.assertCommonized("((a,b), (c,d))", "expect val x: Int")
        result.assertCommonized("(a, b)", "expect val x: Int")
        result.assertCommonized("(c, d)", "expect val x: Int")
        result.assertCommonized("a", "val x: Int = 42")
        result.assertCommonized("b", "val x: Int = 42")
        result.assertCommonized("c", "val x: Int = 42")
        result.assertCommonized("d", "val x: Int = 42")
    }
}
