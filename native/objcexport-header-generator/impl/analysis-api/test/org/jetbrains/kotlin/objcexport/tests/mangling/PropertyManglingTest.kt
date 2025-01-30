package org.jetbrains.kotlin.objcexport.tests.mangling

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.mangling.mangleObjCProperties
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.translateToObjCExportStub
import org.jetbrains.kotlin.objcexport.withKtObjCExportSession
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PropertyManglingTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - method and property have the same name`() {
        doTest(
            """
            class Foo {
                val bar: Int = 42
                fun bar() = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val properties = translateToObjCExportStub(foo)?.members ?: error("no translated members")
            val property = mangleObjCProperties(properties).filterIsInstance<ObjCProperty>().first()

            assertEquals("bar", property.name)
            assertEquals(listOf("readonly", "getter=bar_"), property.propertyAttributes)
        }
    }

    @Test
    fun `test - method and property have the same name, but method has 1 parameter`() {
        doTest(
            """
            class Foo {
                val bar: Int = 42
                fun bar(value: Boolean) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val properties = translateToObjCExportStub(foo)?.members ?: error("no translated members")
            val property = mangleObjCProperties(properties).filterIsInstance<ObjCProperty>().first()

            assertEquals("bar", property.name)
            assertEquals(listOf("readonly"), property.propertyAttributes)
        }
    }

    private fun doTest(@Language("kotlin") code: String, run: ObjCExportContext.(KaClassSymbol) -> Unit) {
        val file = inlineSourceCodeAnalysis.createKtFile(code.trimMargin())
        analyze(file) {
            val foo = getClassOrFail(file, "Foo")
            val kaSession = this
            withKtObjCExportSession(KtObjCExportConfiguration()) {
                with(ObjCExportContext(analysisSession = kaSession, exportSession = this)) {
                    run(foo)
                }
            }
        }
    }
}