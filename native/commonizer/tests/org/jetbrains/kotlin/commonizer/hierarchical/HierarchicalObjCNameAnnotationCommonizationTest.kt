/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.jupiter.api.Test

class HierarchicalObjCNameAnnotationCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    @Test
    fun `test ObjCName on class`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "UserDefaults")
                    class NSUserDefaults
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "UserDefaults")
                    class NSUserDefaults
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                @kotlin.native.ObjCName(swiftName = "UserDefaults")
                expect class NSUserDefaults()
            """.trimIndent()
        )
    }

    @Test
    fun `test ObjCName on interface`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "URLSessionDelegate")
                    interface NSURLSessionDelegate
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "URLSessionDelegate")
                    interface NSURLSessionDelegate
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                @kotlin.native.ObjCName(swiftName = "URLSessionDelegate")
                expect interface NSURLSessionDelegate
            """.trimIndent()
        )
    }

    @Test
    fun `test ObjCName with different swiftName is dropped`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "UserDefaults")
                    class NSUserDefaults
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    @file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
                    @kotlin.native.ObjCName(swiftName = "Defaults")
                    class NSUserDefaults
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class NSUserDefaults()
            """.trimIndent()
        )
    }
}
