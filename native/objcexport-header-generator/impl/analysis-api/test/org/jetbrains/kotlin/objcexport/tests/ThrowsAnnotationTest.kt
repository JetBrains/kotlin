package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefinedThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getEffectiveThrows
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
            assertTrue(getFunctionOrFail(file, "foo").hasThrowsAnnotation)
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
            val classA = getClassOrFail(file, "A")
            val classB = getClassOrFail(file, "B")

            assertTrue(getFunctionOrFail(classA, "foo").hasThrowsAnnotation)
            assertFalse(getFunctionOrFail(classB, "foo").hasThrowsAnnotation)
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
            val foo = getFunctionOrFail(file, "foo")
            assertEquals(listOf("IllegalStateException", "RuntimeException"), getDefinedThrows(foo).mapName())
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

            val classA = getClassOrFail(file, "A")
            val fooA = getFunctionOrFail(classA.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), getEffectiveThrows(fooA).mapName())
            assertEquals(listOf("IllegalStateException"), getDefinedThrows(fooA).mapName())

            val classB = getClassOrFail(file, "B")
            val fooB = getFunctionOrFail(classB.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), getEffectiveThrows(fooB).mapName())
            assertEquals(listOf("RuntimeException"), getDefinedThrows(fooB).mapName())

            val classC = getClassOrFail(file, "C")
            val fooC = getFunctionOrFail(classC.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), getEffectiveThrows(fooC).mapName())
            assertEquals(listOf("IndexOutOfBoundsException"), getDefinedThrows(fooC).mapName())
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
            val foo = getClassOrFail(file, "Foo").memberScope.constructors.first()
            assertEquals(listOf("IllegalStateException"), getEffectiveThrows(foo).mapName())
            assertEquals(listOf("IllegalStateException"), getDefinedThrows(foo).mapName())
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
            val foo = getFunctionOrFail(file, "foo")
            assertEquals(listOf("RuntimeException"), getDefinedThrows(foo).mapName())
        }
    }
}

private fun List<ClassId>.mapName(): List<String> {
    return map { it.shortClassName.asString() }
}