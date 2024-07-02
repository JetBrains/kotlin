package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasExportForCompilerAnnotation
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class HasExportForCompilerAnnotationTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - has ExportForCompiler annotation`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo
            val array: Array<Int>
            val iterator: Iterator<Int>
            val foo: Foo
        """.trimIndent()
        )

        analyze(ktFile) {
            assertTrue(
                verifyHasExportForCompilerAnnotation(ktFile.getPropertyOrFail("array", this))
            )
            assertFalse(
                verifyHasExportForCompilerAnnotation(ktFile.getPropertyOrFail("iterator", this))
            )
            assertFalse(
                verifyHasExportForCompilerAnnotation(ktFile.getPropertyOrFail("foo", this))
            )
        }
    }
}

@OptIn(KaExperimentalApi::class)
private fun KaSession.verifyHasExportForCompilerAnnotation(property: KaPropertySymbol): Boolean {
    return property
        .returnType
        .scope?.getConstructors()?.toList()?.any { it.hasExportForCompilerAnnotation }
        ?: fail("Property return type has no constructors: ${property.returnType}")
}