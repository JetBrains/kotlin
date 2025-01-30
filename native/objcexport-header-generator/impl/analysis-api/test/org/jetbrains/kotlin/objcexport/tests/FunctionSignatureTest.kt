package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getStringSignature
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FunctionSignatureTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - simple function`() {
        doTest(
            "fun foo() = Unit",
            "foo(){}"
        )
    }

    @Test
    fun `test - one param`() {
        doTest(
            "fun foo(a: Int) = Unit",
            "foo(kotlin.Int){}"
        )
    }

    @Test
    fun `test - nullable param`() {
        doTest(
            "fun foo(a: Int?) = Unit",
            "foo(kotlin.Int?){}"
        )
    }

    @Test
    fun `test - two params`() {
        doTest(
            "fun foo(a: Int, b: String) = Unit",
            "foo(kotlin.Int;kotlin.String){}"
        )
    }

    @Test
    fun `test - class param`() {
        doTest(
            """
            interface Foo
            fun foo(foo: Foo) = Unit
            """,
            "foo(Foo){}"
        )
    }

    @Test
    fun `test - generic param`() {
        doTest(
            "fun <T> foo(t: T) = Unit",
            "foo(0:0){0§<kotlin.Any?>}"
        )
    }

    @Test
    fun `test - func param`() {
        doTest(
            "fun foo(param: () -> Unit) = Unit",
            "foo(kotlin.Function0<kotlin.Unit>){}"
        )
    }

    @Test
    fun `test - func param with param and return type`() {
        doTest(
            "fun foo(param: (String) -> Int) = Unit",
            "foo(kotlin.Function1<kotlin.String,kotlin.Int>){}"
        )
    }

    @Test
    fun `test - unused generics`() {
        doTest(
            "fun <A, B, C> foo(b: A) = Unit",
            "foo(0:0){0§<kotlin.Any?>;1§<kotlin.Any?>;2§<kotlin.Any?>}"
        )
    }

    @Test
    fun `test - upper bound`() {
        doTest(
            """
            interface Bar
            fun <A : Bar> foo(a: A) = Unit
            """,
            "foo(0:0){0§<Bar>}"
        )
    }

    @Test
    fun `test - multiple upper bounds`() {
        doTest(
            """        
            interface Foo
            interface Bar
            fun <T> foo(t: T) where T : Foo, T : Bar = Unit
            """,
            "foo(0:0){0§<Foo&Bar>}"
        )
    }

    @Test
    fun `test - primitive varargs`() {
        doTest(
            "fun foo(vararg v: Int) = Unit",
            "foo(kotlin.IntArray...){}"
        )
    }

    @Test
    fun `test - non primitive varargs`() {
        doTest(
            """
            interface Foo
            fun foo(vararg v: Foo) = Unit
            """,
            "foo(kotlin.Array<out|Foo>...){}"
        )
    }

    @Test
    fun `test - typed interface func`() {
        doTest(
            """
            interface TypedInterface<T>
            fun foo(t: TypedInterface<String>) = Unit
            """,
            "foo(TypedInterface<kotlin.String>){}"
        )
    }

    private fun doTest(
        @Language("kotlin") code: String,
        expected: String,
    ) {
        val file = createTestFile(code.trimIndent())
        analyze(file) {
            assertEquals(
                expected,
                getStringSignature(file.getFunctionOrFail("foo", this))
            )
        }
    }

    private fun createTestFile(@Language("kotlin") code: String): KtFile {
        return inlineSourceCodeAnalysis.createKtFile(code.trimIndent())
    }
}