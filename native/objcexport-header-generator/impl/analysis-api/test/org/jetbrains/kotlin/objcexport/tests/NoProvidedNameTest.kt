package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.testUtils.createObjCExportFile
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class NoProvidedNameTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - class without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("class") { exportFile ->
            assertNull(translateToObjCClass(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - interface without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("interface") { exportFile ->
            assertNull(translateToObjCProtocol(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - object without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("object") { exportFile ->
            assertNull(translateToObjCObject(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - enum class without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("enum class") { exportFile ->
            assertNull(translateToObjCClass(exportFile.classifierSymbols.first()))
        }
    }

    @Test
    fun `test - fun without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("fun") { exportFile ->
            assertNull(translateToObjCMethod(exportFile.callableSymbols.first() as KaFunctionSymbol))
        }
    }

    @Test
    fun `test - val without name is not translated`() {
        inlineSourceCodeAnalysis.createObjCExportFile("val") { exportFile ->
            assertNull(translateToObjCProperty(exportFile.callableSymbols.first() as KaPropertySymbol))
        }
    }
}