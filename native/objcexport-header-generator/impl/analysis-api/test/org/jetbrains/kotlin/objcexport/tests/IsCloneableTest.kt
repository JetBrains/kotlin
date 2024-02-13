package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.implementsCloneable
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isClone
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCloneable
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsCloneableTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - is cloneable`() {
        val file = inlineSourceCodeAnalysis.createKtFile("val foo: Cloneable")
        analyze(file) {
            val foo = file.getPropertyOrFail("foo")
            assertTrue(foo.getter?.returnType?.expandedClassSymbol?.isCloneable ?: true)
        }
    }

    @Test
    fun `test - is iterator cloneable`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class A
            val foo: A
        """.trimIndent()
        )
        analyze(file) {
            val foo = file.getPropertyOrFail("foo")
            assertFalse(foo.getter?.returnType?.expandedClassSymbol?.isCloneable ?: true)
        }
    }

    @Test
    fun `test - fake clone method`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface FakeCloneable {
                fun clone(): Any
            }
            class Foo : FakeCloneable {
                override fun clone(): Any {
                    return "any"
                }
            }
        """.trimIndent()
        )
        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            assertFalse(foo.getFunctionOrFail("clone").isClone)
        }
    }

    @Test
    fun `test - implements cloneable`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo : Cloneable {
                override fun clone(): Foo {
                    return this
                }
            }
        """.trimIndent()
        )
        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            assertTrue(foo.implementsCloneable)
            assertTrue(foo.getFunctionOrFail("clone").isClone)
        }
    }

    @Test
    fun `test - fake clone function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            fun clone(): Any {}
        """.trimIndent()
        )
        analyze(file) {
            val clone = file.getFunctionOrFail("clone")
            assertFalse(clone.isClone)
        }
    }

    @Test
    fun `test - clone method`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            val array: Array<Int>
        """.trimIndent()
        )
        analyze(file) {
            assertTrue(
                file
                    .getPropertyOrFail("array")
                    .getter?.returnType?.expandedClassSymbol?.getMemberScope()
                    ?.getFunctionOrFail("clone")
                    ?.isClone ?: false
            )
        }
    }
}