/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.K1AnalysisResult
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import androidx.compose.compiler.plugins.kotlin.k1.allowsComposableCalls
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(b/282189431): run this test with K2
@RunWith(JUnit4::class)
class ScopeComposabilityTests : AbstractCodegenTest(useFir = false) {
    @Test
    fun testNormalFunctions() = assertComposability(
        """
            import androidx.compose.runtime.*

            fun Foo() {
                <normal>
            }
            class Bar {
                fun bam() { <normal> }
                val baz: Int get() { <normal>return 123 }
            }
        """
    )

    @Test
    fun testPropGetter() = assertComposability(
        """
            import androidx.compose.runtime.*

            val baz: Int get() { <normal>return 123 }
        """
    )

    @Test
    fun testBasicComposable() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Foo() {
                <composable>
            }
        """
    )

    @Test
    fun testBasicComposable2() = assertComposability(
        """
            import androidx.compose.runtime.*

            val foo = @Composable { <composable> }

            @Composable
            fun Bar() {
                <composable>
                fun bam() { <normal> }
                val x = { <normal> }
                val y = @Composable { <composable> }
                @Composable fun z() { <composable> }
            }
        """
    )

    // We only analyze scopes that contain composable calls, so this test fails without the
    // nested call to `Bar`. This is why this test was originally muted (b/147250515).
    @Test
    fun testBasicComposable3() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Bar() {
                <composable>
                listOf(1, 2, 3).forEach {
                    <composable>Bar()
                }
            }
        """
    )

    @Test
    fun testBasicComposable4() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable fun Wrap(block: @Composable () -> Unit) { block() }

            @Composable
            fun Bar() {
                <composable>
                Wrap {
                    <composable>
                    Wrap {
                        <composable>
                    }
                }
            }
        """
    )

    @Test
    fun testBasicComposable5() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable fun Callback(block: () -> Unit) { block() }

            @Composable
            fun Bar() {
                <composable>
                Callback {
                    <normal>
                }
            }
        """
    )

    @Test
    fun testBasicComposable6() = assertComposability(
        """
            import androidx.compose.runtime.*

            fun kickOff(block: @Composable () -> Unit) {  }

            fun Bar() {
                <normal>
                kickOff {
                    <composable>
                }
            }
        """
    )

    private fun assertComposability(srcText: String) {
        val (text, carets) = extractCarets(srcText)

        val analysisResult = analyze(listOf(SourceFile("test.kt", text))) as K1AnalysisResult
        val bindingContext = analysisResult.bindingContext
        val ktFile = analysisResult.files.single()

        carets.forEachIndexed { index, (offset, marking) ->
            val composable = ktFile.findElementAt(offset)!!.getNearestComposability(bindingContext)

            when (marking) {
                "<composable>" -> assertTrue("index: $index", composable)
                "<normal>" -> assertTrue(
                    "index: $index",
                    !composable
                )
                else -> error("Composability of $marking not recognized.")
            }
        }
    }

    private val callPattern = Regex("(<composable>)|(<normal>)")
    private fun extractCarets(text: String): Pair<String, List<Pair<Int, String>>> {
        val indices = mutableListOf<Pair<Int, String>>()
        var offset = 0
        val src = callPattern.replace(text) {
            indices.add(it.range.first - offset to it.value)
            offset += it.range.last - it.range.first + 1
            ""
        }
        return src to indices
    }
}

fun PsiElement?.getNearestComposability(
    bindingContext: BindingContext
): Boolean {
    var node: PsiElement? = this
    while (node != null) {
        when (node) {
            is KtFunctionLiteral -> {
                // keep going, as this is a "KtFunction", but we actually want the
                // KtLambdaExpression
            }
            is KtLambdaExpression -> {
                val descriptor = bindingContext[BindingContext.FUNCTION, node.functionLiteral]
                    ?: return false
                return descriptor.allowsComposableCalls(bindingContext)
            }
            is KtFunction,
            is KtPropertyAccessor,
            is KtProperty -> {
                val descriptor = bindingContext[BindingContext.FUNCTION, node] ?: return false
                return descriptor.allowsComposableCalls(bindingContext)
            }
        }
        node = node.parent as? KtElement
    }
    return false
}
