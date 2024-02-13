package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.needsCompanionProperty
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompanionTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - needs companion property`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class NoCompanion {
            
            }
            
            class EmptyCompanion {
                companion object {}
            }
            
            class SimpleCompanion {
                companion object {
                    const val a = 42
                }
            }
        """.trimIndent()
        )
        analyze(file) {
            assertFalse(file.getClassOrFail("NoCompanion").needsCompanionProperty)
            assertTrue(file.getClassOrFail("EmptyCompanion").needsCompanionProperty)
            assertTrue(file.getClassOrFail("SimpleCompanion").needsCompanionProperty)
        }
    }
}