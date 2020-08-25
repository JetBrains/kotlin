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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext

class ScopeComposabilityTests : AbstractCodegenTest() {

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

    fun testPropGetter() = assertComposability(
        """
            import androidx.compose.runtime.*

            val baz: Int get() { <normal>return 123 }
        """
    )

    fun testBasicComposable() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Foo() {
                <composable>
            }
        """
    )

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

    // TODO(b/147250515): get inlined lambdas to analyze correctly
    fun xtestBasicComposable3() = assertComposability(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Bar() {
                <composable>
                listOf(1, 2, 3).forEach {
                    <composable>
                }
            }
        """
    )

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

    private fun <T> setup(block: () -> T): T {
        return block()
    }

    fun assertComposability(srcText: String) = setup {
        val (text, carets) = extractCarets(srcText)

        val environment = myEnvironment ?: error("Environment not initialized")

        val ktFile = KtPsiFactory(environment.project).createFile(text)
        val bindingContext = JvmResolveUtil.analyze(
            ktFile,
            environment
        ).bindingContext

        carets.forEachIndexed { index, (offset, marking) ->
            val composable = composabiliityAtOffset(bindingContext, ktFile, offset)

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

    private val callPattern = Regex("(<marked>)|(<inferred>)|(<normal>)")
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

    private fun composabiliityAtOffset(
        bindingContext: BindingContext,
        jetFile: KtFile,
        index: Int
    ): Boolean {
        val element = jetFile.findElementAt(index)!!
        return element.getNearestComposability(bindingContext)
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
            is KtLambdaExpression,
            is KtFunction,
            is KtPropertyAccessor,
            is KtProperty -> {
                val descriptor = bindingContext[BindingContext.FUNCTION, node]
                if (descriptor == null) {
                    return false
                }
                return descriptor.allowsComposableCalls(bindingContext)
            }
        }
        node = node.parent as? KtElement
    }
    return false
}