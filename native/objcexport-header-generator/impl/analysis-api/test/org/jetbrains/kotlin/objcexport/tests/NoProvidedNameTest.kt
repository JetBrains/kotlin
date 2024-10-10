package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.toObjCExportFile
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class NoProvidedNameTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - class without name is not translated`() {
        "class".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCClass(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - interface without name is not translated`() {
        "interface".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCProtocol(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - object without name is not translated`() {
        "object".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCObject(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - enum class without name is not translated`() {
        "enum class".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCClass(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - fun without name is not translated`() {
        "fun".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCMethod(exportFile.callableSymbols.first() as KaFunctionSymbol))
        }
    }

    @Test
    fun `test - val without name is not translated`() {
        "val".toObjCExportFile(inlineSourceCodeAnalysis) { exportFile ->
            assertNull(translateToObjCProperty(exportFile.callableSymbols.first() as KaPropertySymbol))
        }
    }
}