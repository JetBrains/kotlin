/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalTypeAliasCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple type alias`() {
        val result = commonize {
            outputTarget("((a,b), (c,d))")
            simpleSingleSourceTarget("a", "typealias X = Int")
            simpleSingleSourceTarget("b", "typealias X = Int")
            simpleSingleSourceTarget("c", "typealias X = Int")
            simpleSingleSourceTarget("d", "typealias X = Int")
        }

        result.assertCommonized("((a,b), (c,d))", "typealias X = Int")
        result.assertCommonized("(a,b)", "")
        result.assertCommonized("(c, d)", "")
        result.assertCommonized("a", "")
        result.assertCommonized("b", "")
        result.assertCommonized("c", "")
        result.assertCommonized("d", "")
    }
}
