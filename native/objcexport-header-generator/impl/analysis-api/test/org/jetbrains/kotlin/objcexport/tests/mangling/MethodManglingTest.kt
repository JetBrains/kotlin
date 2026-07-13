package org.jetbrains.kotlin.objcexport.tests.mangling

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.mangling.mangleObjCMethods
import org.jetbrains.kotlin.export.test.getClassOrFail
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
            val stub = translateToObjCExportStub(foo)?.objCClass
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
            val stub = translateToObjCExportStub(foo)?.objCClass
            val methods = stub?.members ?: error("no translated members")
            val mangledMethods = mangleObjCMethods(methods, stub).filterIsInstance<ObjCMethod>().filter { it.name.startsWith("bar") }

            assertEquals("barValue:value:", mangledMethods[0].name)
            assertEquals("barValue:value_:", mangledMethods[1].name)
        }
    }

    @Test
    fun `test - clash between throws and non-throws`() {
        doTest(
            """
            class Foo {
                @Throws(Throwable::class)
                fun foo1(x: Int) {}
    
                fun foo1(x: Long) {}
                
                // Same, but reversed: the second method has `@Throws`, not the first one.
                fun foo2(x: Int) {}
                
                @Throws(Throwable::class)
                fun foo2(x: Long) {}
            }
        """.trimMargin()
        ) { foo ->
            val stub = translateToObjCExportStub(foo)?.objCClass
            val methods = stub?.members ?: error("no translated members")
            val mangledMethods = mangleObjCMethods(methods, stub)
                .filterIsInstance<ObjCMethod>()
                .filter { it.name.startsWith("foo") }

            fun swiftName(swiftName: String) = listOf("swift_name(\"$swiftName\")")

            assertEquals(swiftName("foo1(x:)"), mangledMethods[0].attributes)
            assertEquals("foo1X:error:", mangledMethods[0].name)

            assertEquals(swiftName("foo1(x_:)"), mangledMethods[1].attributes)
            assertEquals("foo1X_:", mangledMethods[1].name) // This is actually not right, see KT-86289.

            assertEquals(swiftName("foo2(x:)"), mangledMethods[2].attributes)
            assertEquals("foo2X:", mangledMethods[2].name)

            assertEquals(swiftName("foo2(x_:)"), mangledMethods[3].attributes)
            assertEquals("foo2X:error_:", mangledMethods[3].name) // This is actually not right, see KT-86289.
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
