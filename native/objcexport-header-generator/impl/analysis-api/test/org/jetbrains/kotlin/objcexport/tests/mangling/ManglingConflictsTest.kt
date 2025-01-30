package org.jetbrains.kotlin.objcexport.tests.mangling

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.mangling.hasMethodConflicts
import org.jetbrains.kotlin.objcexport.mangling.hasPropertiesConflicts
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.translateToObjCExportStub
import org.jetbrains.kotlin.objcexport.withKtObjCExportSession
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ManglingConflictsTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - hasPropertiesConflicts returns true`() {
        doTest(
            """
            class Foo {
                val bar: Int = 42
                fun bar() = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val members = translateToObjCExportStub(foo)?.members ?: error("no translated members")
            assertTrue(members.hasPropertiesConflicts())
        }
    }

    @Test
    fun `test - hasPropertiesConflicts returns false`() {
        doTest(
            """
            class Foo {
                val bar: Int = 42
                fun bar(value: Boolean) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)
            assertTrue(stub?.members?.hasPropertiesConflicts() == false)
        }
    }

    @Test
    fun `test - hasMethodConflicts returns true`() {
        doTest(
            """
            class Foo {
                fun bar(value: Int) = Unit
                fun bar(value: Boolean) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)
            assertTrue(stub?.members?.hasMethodConflicts() == true)
        }
    }

    @Test
    fun `test - hasMethodConflicts returns false`() {
        doTest(
            """
            class Foo {
                fun bar(value0: Boolean) = Unit
                fun bar(value0: Boolean, value1: Boolean) = Unit
            }            
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)
            assertTrue(stub?.members?.hasPropertiesConflicts() == false)
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