/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder

class HierarchicalCInteropCallableAnnotationCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test ObjCMethod annotation`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)") {
                objCAnnotations()
            }

            simpleSingleSourceTarget(
                "a", """
                    import kotlinx.cinterop.*
                    @ObjCMethod("x", "xab", false)
                    fun x() {}
                    @ObjCMethod("wrongSelector1", "", false)
                    fun y() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    import kotlinx.cinterop.*
                    @ObjCMethod("x", "xab", false)
                    fun x() {}
                    @ObjCMethod("wrongSelector2", "", false)
                    fun y() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                    import kotlinx.cinterop.*
                    @ObjCMethod("x", "xc", true)
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                    import kotlinx.cinterop.*
                    @ObjCMethod("x", "xd")
                    fun x() {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                import kotlin.commonizer.*
                import kotlinx.cinterop.*
                @ObjCCallable @ObjCMethod("x", "xab", false)
                expect fun x()
                @ObjCCallable
                expect fun y()
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                import kotlin.commonizer.*
                import kotlinx.cinterop.*
                @ObjCCallable @ObjCMethod("x", "")
                expect fun x()
            """.trimIndent()
        )

        result.assertCommonized(
            "(a, b, c, d)", """
                import kotlin.commonizer.*
                import kotlinx.cinterop.*
                @ObjCCallable @ObjCMethod("x", "")
                expect fun x()
            """.trimIndent()
        )
    }

    fun `test only ObjCCallable annotation`() {
        val result = commonize {
            outputTarget("(a, b)")
            registerDependency("a", "b", "(a, b)") {
                objCAnnotations()
            }

            simpleSingleSourceTarget(
                "a", """
                    import kotlin.commonizer.*
                    @ObjCCallable
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    import kotlin.commonizer.*
                    @ObjCCallable
                    fun x() {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.commonizer.*
            @ObjCCallable
            expect fun x()
        """.trimIndent()
        )
    }

    fun `test multiple objC annotations`() {
        val result = commonize {
            outputTarget("(a, b)", "(c, d)", "(a, b, c, d)")
            registerDependency("a", "b", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)") {
                objCAnnotations()
            }

            simpleSingleSourceTarget(
                "a", """
                    import kotlinx.cinterop.*
                    @ObjCMethod("x", "xa")
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    import kotlinx.cinterop.*
                    @ObjCConstructor("x", false)
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                    import kotlinx.cinterop.*
                    @ObjCFactory("x", "xa")
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                    import kotlin.commonizer.*
                    @ObjCCallable
                    fun x() {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                import kotlin.commonizer.*
                @ObjCCallable
                expect fun x()
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                import kotlin.commonizer.*
                @ObjCCallable
                expect fun x()
            """.trimIndent()
        )

        result.assertCommonized(
            "(a, b, c, d)", """
                import kotlin.commonizer.*
                @ObjCCallable
                expect fun x()
            """.trimIndent()
        )
    }

    fun `test single platform not marked as objc`() {
        val result = commonize {
            registerDependency("a", "b", "c", "d", "(a, b)", "(c, d)") { objCAnnotations() }
            outputTarget("(a, b)", "(c, d)")

            simpleSingleSourceTarget(
                "a", """
                    import kotlinx.cinterop.*
                    
                    @ObjCMethod("x", "xa")
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "c", """
                    import kotlin.commonizer.*
                    
                    @ObjCCallable
                    fun x() {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "d", """
                    fun x() {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect fun x()
            """.trimIndent()
        )

        result.assertCommonized(
            "(c, d)", """
                expect fun x()
            """.trimIndent()
        )
    }

}

private fun InlineSourceBuilder.ModuleBuilder.objCAnnotations() {
    // Register fake kotlinx.cinterop annotations.
    //  Annotations contain a parameter x which is used to make them "not commonizable"
    source(
        """
        package kotlinx.cinterop
        public annotation class ObjCMethod(val selector: String, val encoding: String, val isStret: Boolean = false)
        public annotation class ObjCConstructor(val initSelector: String, val designated: Boolean)
        public annotation class ObjCFactory(val selector: String, val encoding: String, val isStret: Boolean = false)
        """.trimIndent(),
        "kotlinx.kt"
    )

    source(
        """
        package kotlin.commonizer
        annotation class ObjCCallable
        """.trimIndent()
    )
}