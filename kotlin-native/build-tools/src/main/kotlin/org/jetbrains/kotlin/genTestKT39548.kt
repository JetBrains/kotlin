package org.jetbrains.kotlin

import java.io.File

fun genTestKT39548(file: File) {
    val longName = StringBuilder().apply {
        repeat(10_000_000) {
            append('a')
        }
    }

    val text = """
            import kotlin.test.*

            fun $longName(): Int = 42
            fun <T> same(value: T): T = value
            val globalInt1: Int = same(1)
            val globalStringA: String = same("a")
            @ThreadLocal val threadLocalInt2: Int = same(2)
            @ThreadLocal val threadLocalStringB: String = same("b")

            fun main() {
                // Ensure function don't get DCEd:
                val resultOfFunctionWithLongName = $longName()
                assertEquals(42, resultOfFunctionWithLongName)

                // Check that top-level initializers did run as expected:
                assertEquals(1, globalInt1)
                assertEquals("a", globalStringA)
                assertEquals(2, threadLocalInt2)
                assertEquals("b", threadLocalStringB)
            }
        """.trimIndent()

    file.parentFile.mkdirs()
    file.writeText(text)
}
