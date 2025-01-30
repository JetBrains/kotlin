package org.jetbrains.kotlin.objcexport.tests.mangling

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.mangling.mangleObjCMethods
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.translateToObjCExportStub
import org.jetbrains.kotlin.objcexport.withKtObjCExportSession
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MethodManglingTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - method with one parameter`() {
        doTest(
            """
            class Foo {
                fun bar(value: Int) = Unit
                fun bar(value: Boolean) = Unit
                fun bar(value: String) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)
            val methods = stub?.members ?: error("no translated members")
            val mangledMethods = mangleObjCMethods(methods, stub).filterIsInstance<ObjCMethod>().filter { it.name.startsWith("bar") }

            assertEquals("barValue:", mangledMethods[0].name)
            assertEquals("barValue_:", mangledMethods[1].name)
            assertEquals("barValue__:", mangledMethods[2].name)
        }
    }

    @Test
    fun `test - method with two parameters`() {
        doTest(
            """
            class Foo {
                fun bar(value: Int, value: Boolean) = Unit
                fun bar(value: Boolean, value: Int) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)
            val methods = stub?.members ?: error("no translated members")
            val mangledMethods = mangleObjCMethods(methods, stub).filterIsInstance<ObjCMethod>().filter { it.name.startsWith("bar") }

            assertEquals("barValue:value:", mangledMethods[0].name)
            assertEquals("barValue:value_:", mangledMethods[1].name)
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