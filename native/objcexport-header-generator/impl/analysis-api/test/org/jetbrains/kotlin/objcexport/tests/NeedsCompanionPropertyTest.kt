package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.getNeedsCompanionProperty
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeedsCompanionPropertyTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - no companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class NoCompanion {
            
            }
        """.trimIndent()
        )
        analyze(file) {
            assertFalse(getNeedsCompanionProperty(getClassOrFail(file, "NoCompanion")))
        }
    }

    @Test
    fun `test - empty companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """            
            class EmptyCompanion {
                companion object {}
            }
        """.trimIndent()
        )
        analyze(file) {
            assertTrue(getNeedsCompanionProperty(getClassOrFail(file, "EmptyCompanion")))
        }
    }

    @Test
    fun `test - simple companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class SimpleCompanion {
                companion object {
                    const val a = 42
                }
            }
        """.trimIndent()
        )
        analyze(file) {
            assertTrue(getNeedsCompanionProperty(getClassOrFail(file, "SimpleCompanion")))
        }
    }
}