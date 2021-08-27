/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder

class HierarchicalUnsafeNumberAnnotationCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test multiple targets`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)") { unsafeNumberAnnotationSource() }
            simpleSingleSourceTarget("a", "typealias X = Short")
            simpleSingleSourceTarget("b", "typealias X = Int")
            simpleSingleSourceTarget("c", "typealias X = Int")
            simpleSingleSourceTarget("d", "typealias X = Long")
        }

        result.assertCommonized(
            "(a, b)", """
                @kotlinx.cinterop.UnsafeNumber(["a: kotlin/Short", "b: kotlin/Int"])
                typealias X = Int
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                import kotlinx.cinterop.*
                @UnsafeNumber(["c: kotlin/Int", "d: kotlin/Long"])
                typealias X = Long
            """.trimIndent()
        )

        result.assertCommonized(
            "(a, b, c, d)", """
                import kotlinx.cinterop.*
                @UnsafeNumber(["a: kotlin/Short", "b: kotlin/Int", "c: kotlin/Int", "d: kotlin/Long"])
                typealias X = Long
            """.trimIndent()
        )
    }
}

private fun InlineSourceBuilder.ModuleBuilder.unsafeNumberAnnotationSource() {
    source(
        """
            package kotlinx.cinterop
            @Target(AnnotationTarget.TYPEALIAS)
            @Retention(AnnotationRetention.BINARY)
            annotation class UnsafeNumber(val actualPlatformTypes: Array<String>)
        """.trimIndent(),
        "UnsafeNumberAnnotation.kt"
    )
}