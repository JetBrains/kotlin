package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasErrorTypes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasErrorTypesTest(
    private val headerGenerator: HeaderGenerator,
) {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `test - no errors`() {
        val stubs = stubs(
            """
            fun foo() = Unit
            class Foo {
                val myProperty: Int = 42
                fun myFunction(): Int = 42
            }
        """.trimIndent()
        )

        assertFalse(stubs.hasErrorTypes())
    }

    @Test
    fun `test - property return type error`() {
        val stubs = stubs(
            """
            val foo: Unresolved get() = error("stub")
        """.trimIndent()
        )

        assertTrue(stubs.hasErrorTypes())
    }

    @Test
    fun `test - function return type error`() {
        val stubs = stubs(
            """
        fun foo(): Unresolved = error("stub")
        """.trimIndent()
        )

        assertTrue(stubs.hasErrorTypes())
    }

    @Test
    fun `test - nested member function return type error`() {
        val stubs = stubs(
            """
            class Foo {
                fun foo(): Unresolved = error("stub")
            }
        """.trimIndent()
        )

        assertTrue(stubs.hasErrorTypes())
    }

    @Test
    fun `test - nested member property return type error`() {
        val stubs = stubs(
            """
            class Foo {
                val foo: Unresolved get() = error("stub")
            }
        """.trimIndent()
        )

        assertTrue(stubs.hasErrorTypes())
    }

    @Test
    fun `test - nested error class property`() {
        val stubs = stubs(
            """
            class A {
              class B {
                val e: Unresolved get() = error("stub")
              }
            }
        """.trimIndent()
        )

        assertTrue(stubs.hasErrorTypes())
    }

    private fun stubs(@Language("kotlin") sourceCode: String): List<ObjCExportStub> {
        val sourceFile = tempDir.resolve("sources.kt")
        sourceFile.writeText(sourceCode)
        return headerGenerator.generateHeaders(tempDir.toFile()).stubs
    }
}
