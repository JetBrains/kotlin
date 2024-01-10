package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isTopLevel
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsTopLevelTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - top level fun`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            fun topFun() {}
            class TopClass {
                fun classFun() {}
            }
        """.trimIndent()
        )

        analyze(ktFile) {
            assertTrue(ktFile.getFunctionOrFail("topFun").isTopLevel)
            assertFalse (ktFile.getClassOrFail("TopClass").getFunctionOrFail("classFun").isTopLevel)
        }
    }
}