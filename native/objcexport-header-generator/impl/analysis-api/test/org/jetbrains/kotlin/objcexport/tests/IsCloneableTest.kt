package org.jetbrains.kotlin.objcexport.tests

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCloneable
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.junit.jupiter.api.Test

class IsCloneableTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - is cloneable`() {
        val file = inlineSourceCodeAnalysis.createKtFile("val foo: Cloneable")
        analyze(file) {
            val foo = file.getPropertyOrFail("foo")
            assertTrue(foo.type.isCloneable)
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
            assertFalse(foo.type.isCloneable)
        }
    }
}

context(KtAnalysisSession)
private val KtPropertySymbol.type: KtClassOrObjectSymbol
    get() {
        return getter?.returnType?.expandedClassSymbol!!
    }