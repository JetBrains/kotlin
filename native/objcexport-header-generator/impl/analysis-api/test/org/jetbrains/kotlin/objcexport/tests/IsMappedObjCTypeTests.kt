package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.objcexport.isMappedObjCType
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertFalse
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.Test

class IsMappedObjCTypeTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - basic class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo".trimMargin())
        analyze(file) {
            assertFalse(isMappedObjCType(file.getClassOrFail("Foo", this).defaultType))
        }
    }

    @Test
    fun `test - list`() {
        val file = inlineSourceCodeAnalysis.createKtFile("fun List<Any>.listFoo() = Unit")
        analyze(file) {
            assertTrue(isMappedObjCType(file.getFunctionOrFail("listFoo", this).receiverType))
        }
    }
}

private val KaNamedFunctionSymbol.receiverType: KaType get() = receiverParameter?.type ?: error("$name doesn't have receiver")