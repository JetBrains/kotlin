package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.definedThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.effectiveThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasThrowsAnnotation
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThrowsAnnotationTest(private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {

    @Test
    fun `test - has throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Throws
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            assertTrue(file.getFunctionOrFail("foo").hasThrowsAnnotation)
        }
    }

    @Test
    fun `test - override has no throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                open class A {
                  @Throws
                  open fun foo() = Unit
                }
                class B: A() {
                  override fun foo() = Unit
                }
            """.trimIndent()
        )

        analyze(file) {
            assertTrue(file.getClassOrFail("A").getFunctionOrFail("foo").hasThrowsAnnotation)
            assertFalse(file.getClassOrFail("B").getFunctionOrFail("foo").hasThrowsAnnotation)
        }
    }

    @Test
    fun `test - defined function throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Throws(IllegalStateException::class, RuntimeException::class)
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val foo = file.getFunctionOrFail("foo")
            assertEquals(listOf("IllegalStateException", "RuntimeException"), foo.definedThrows.mapName())
        }
    }

    @Test
    fun `test - effective and defined classes throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            abstract class A {
                @Throws(IllegalStateException::class)
                abstract fun foo()
            }
            
            class B : A() {
                @Throws(RuntimeException::class)
                override fun foo() = Unit
            }
            class C : B() {
                @Throws(IndexOutOfBoundsException::class)
                override fun foo() = Unit
            }
            """.trimIndent()
        )

        analyze(file) {

            val fooA = file.getClassOrFail("A").memberScope.getFunctionOrFail("foo")
            assertEquals(listOf("IllegalStateException"), fooA.effectiveThrows.mapName())
            assertEquals(listOf("IllegalStateException"), fooA.definedThrows.mapName())

            val fooB = file.getClassOrFail("B").memberScope.getFunctionOrFail("foo")
            assertEquals(listOf("IllegalStateException"), fooB.effectiveThrows.mapName())
            assertEquals(listOf("RuntimeException"), fooB.definedThrows.mapName())

            val fooC = file.getClassOrFail("C").memberScope.getFunctionOrFail("foo")
            assertEquals(listOf("IllegalStateException"), fooC.effectiveThrows.mapName())
            assertEquals(listOf("IndexOutOfBoundsException"), fooC.definedThrows.mapName())
        }
    }

    @Test
    fun `test - constructor throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo @Throws(IllegalStateException::class) constructor()
            """.trimIndent()
        )

        analyze(file) {
            val foo = file.getClassOrFail("Foo").memberScope.constructors.first()
            assertEquals(listOf("IllegalStateException"), foo.effectiveThrows.mapName())
            assertEquals(listOf("IllegalStateException"), foo.definedThrows.mapName())
        }
    }

    @Test
    fun `test - non throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            annotation class NonThrows(vararg val exceptionClasses: KClass<out Throwable>)

            @Throws(RuntimeException::class)
            @NonThrows(IllegalStateException::class)
            fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val foo = file.getFunctionOrFail("foo")
            assertEquals(listOf("RuntimeException"), foo.definedThrows.mapName())
        }
    }
}

private fun List<ClassId>.mapName(): List<String> {
    return map { it.shortClassName.asString() }
}