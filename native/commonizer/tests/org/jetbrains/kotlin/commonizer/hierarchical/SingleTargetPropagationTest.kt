/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.Test

/**
 * Related https://youtrack.jetbrains.com/issue/KT-46248
 */
class SingleTargetPropagationTest : AbstractInlineSourcesCommonizationTest() {

    /**
     * Following the simple design principle:
     * Absent/Unsupported targets shall result in the same output as a request only mentioning supported targets
     */
    @Test
    fun `test single native target in hierarchy`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            simpleSingleSourceTarget("a", """class A""")
        }

        result.assertCommonized("(a,b)", "expect class A()")
        result.assertCommonized("((a, b), (c, d))", "expect class A()")
    }
}