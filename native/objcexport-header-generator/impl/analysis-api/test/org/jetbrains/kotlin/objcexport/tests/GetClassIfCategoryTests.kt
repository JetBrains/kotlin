/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.export.test.getPropertyOrFail
import org.jetbrains.kotlin.objcexport.getClassIfCategory
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class GetClassIfCategoryTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - null when there is receiver`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Bar
            class Foo {
                fun memberFoo() = Unit
                fun Bar.memberBarExtension() = Unit
                fun String.memberStringExtension() = Unit
                val prop = 42
            }
        """.trimIndent()
        )

        analyze(file) {
            val fooClass = getClassOrFail(file, "Foo")

            assertNull(getClassIfCategory(getFunctionOrFail(fooClass, "memberFoo")))
            assertNull(getClassIfCategory(getFunctionOrFail(fooClass, "memberBarExtension")))
            assertNull(getClassIfCategory(getFunctionOrFail(fooClass, "memberStringExtension")))
            assertNull(getClassIfCategory(getPropertyOrFail(fooClass, "prop")))
        }
    }

    @Test
    fun `test - null when there is no extension and no receiver`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            fun topLevelFoo() = Unit
            val prop = 42
        """.trimMargin()
        )
        analyze(file) {
            assertNull(getClassIfCategory(getFunctionOrFail(file, "topLevelFoo")))
            assertNull(getClassIfCategory(getPropertyOrFail(file, "prop")))
        }
    }

    @Test
    fun `test - null when extension isObjCObjectType == true`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            import kotlinx.cinterop.ObjCObject
            fun ObjCObject.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(getClassIfCategory(getFunctionOrFail(file, "foo")))
        }
    }

    @Test
    fun `test - null when extension is interface`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface Foo
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(getClassIfCategory(getFunctionOrFail(file, "foo")))
        }
    }

    @Test
    fun `test - null when extension type is inlined`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            @JvmInline
            value class Foo(val i: Int)
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(getClassIfCategory(getFunctionOrFail(file, "foo")))
        }
    }

    @Test
    fun `test - null when extension type isSpecialMapped == true`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            fun Any.anyFoo() = Unit
            fun List<String>.listFoo() = Unit
            fun String.stringFoo() = Unit
            fun Function<String>.fooFun() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(getClassIfCategory(getFunctionOrFail(file, "anyFoo")))
            assertNull(getClassIfCategory(getFunctionOrFail(file, "listFoo")))
            assertNull(getClassIfCategory(getFunctionOrFail(file, "stringFoo")))
            assertNull(getClassIfCategory(getFunctionOrFail(file, "fooFun")))
        }
    }

    @Test
    fun `test - not inline class && not any && not mapped`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo(val i: Int)
            fun Foo.bar() = Unit
        """.trimMargin()
        )
        analyze(file) {
            val foo = checkNotNull(getClassIfCategory(getClassOrFail(file, "Foo").defaultType))
            val bar = checkNotNull(getClassIfCategory(getFunctionOrFail(file, "bar")))

            assertEquals("Foo", foo.name?.identifier)
            assertEquals("Foo", bar.name?.identifier)
        }
    }
}
