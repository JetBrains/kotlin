package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.getClassIfCategory
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class GetClassIfCategoryTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - null when there is receiver`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Bar
            class Foo {
                fun memberFoo() = Unit
                fun Bar.memberBarExtension() = Unit
                fun String.memberStringExtension() = Unit
                val prop = 42
            }
        """.trimIndent()
        )

        analyze(file) {
            val fooClass = file.getClassOrFail("Foo")

            assertNull(fooClass.getFunctionOrFail("memberFoo").getClassIfCategory())
            assertNull(fooClass.getFunctionOrFail("memberBarExtension").getClassIfCategory())
            assertNull(fooClass.getFunctionOrFail("memberStringExtension").getClassIfCategory())
            assertNull(fooClass.getPropertyOrFail("prop").getClassIfCategory())
        }
    }

    @Test
    fun `test - null when there is no extension and no receiver`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            fun topLevelFoo() = Unit
            val prop = 42
        """.trimMargin()
        )
        analyze(file) {
            assertNull(file.getFunctionOrFail("topLevelFoo").getClassIfCategory())
            assertNull(file.getPropertyOrFail("prop").getClassIfCategory())
        }
    }

    @Test
    fun `test - null when extension isObjCObjectType == true`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            import kotlinx.cinterop.ObjCObject
            fun ObjCObject.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(file.getFunctionOrFail("foo").getClassIfCategory())
        }
    }

    @Test
    fun `test - null when extension is interface`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface Foo
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(file.getFunctionOrFail("foo").getClassIfCategory())
        }
    }

    @Test
    fun `test - null when extension type is inlined`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            @JvmInline
            value class Foo(val i: Int)
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(file.getFunctionOrFail("foo").getClassIfCategory())
        }
    }

    @Test
    fun `test - null when extension type isSpecialMapped == true`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            fun Any.anyFoo() = Unit
            fun List<String>.listFoo() = Unit
            fun String.stringFoo() = Unit
            fun Function<String>.fooFun() = Unit
        """.trimMargin()
        )
        analyze(file) {
            assertNull(file.getFunctionOrFail("anyFoo").getClassIfCategory())
            assertNull(file.getFunctionOrFail("listFoo").getClassIfCategory())
            assertNull(file.getFunctionOrFail("stringFoo").getClassIfCategory())
            assertNull(file.getFunctionOrFail("fooFun").getClassIfCategory())
        }
    }

    @Test
    fun `test - not inline class && not any && not mapped`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo(val i: Int)
            fun Foo.bar() = Unit
        """.trimMargin()
        )
        analyze(file) {

            val foo = checkNotNull(file.getClassOrFail("Foo").defaultType.getClassIfCategory())
            val bar = checkNotNull(file.getFunctionOrFail("bar").getClassIfCategory())

            assertEquals("Foo", foo.name?.identifier)
            assertEquals("Foo", bar.name?.identifier)
        }
    }
}