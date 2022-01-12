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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import kotlin.reflect.KClass

class ComposeCallResolverTests : AbstractCodegenTest() {

    fun testProperties() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            val foo @Composable get() = 123

            class A {
                val bar @Composable get() = 123
            }

            val A.bam @Composable get() = 123

            @Composable
            fun test() {
                val a = A()
                <call>foo
                a.<call>bar
                a.<call>bam
            }
        """
    )

    fun testBasicCallTypes() = assertInterceptions(
        """
            import androidx.compose.runtime.*
            import android.widget.TextView

            @Composable fun Foo() {}

            fun Bar() {}

            @Composable
            fun test() {
                <call>Foo()
                <normal>Bar()
            }
        """
    )

    fun testReceiverScopeCall() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            @Composable fun Int.Foo() {}

            @Composable
            fun test() {
                val x = 1
                x.<call>Foo()

                with(x) {
                    <call>Foo()
                }
            }
        """
    )

    fun testInvokeOperatorCall() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            @Composable operator fun Int.invoke(y: Int) {}

            @Composable
            fun test() {
                val x = 1
                <call>x(y=10)
            }
        """
    )

    fun testComposableLambdaCall() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            @Composable
            fun test(content: @Composable () -> Unit) {
                <call>content()
            }
        """
    )

    fun testComposableLambdaCallWithGenerics() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            @Composable fun <T> A(value: T, block: @Composable (T) -> Unit) {
                <call>block(value)
            }

            @Composable fun <T> B(
                value: T,
                block: @Composable (@Composable (T) -> Unit) -> Unit
            ) {
                <call>block({ })
            }

            @Composable
            fun test() {
                <call>A(123) { it ->
                    println(it)
                }
                <call>B(123) { it ->
                    <call>it(456)
                }
            }
        """
    )

    // TODO(chuckj): Replace with another nested function call.
    fun xtestMethodInvocations() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            val x = CompositionLocal.of<Int> { 123 }

            @Composable
            fun test() {
                x.<call>Provider(456) {

                }
            }
        """
    )

    fun testReceiverLambdaInvocation() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            class TextSpanScope

            @Composable fun Foo(
                scope: TextSpanScope, 
                composable: @Composable TextSpanScope.() -> Unit
            ) {
                with(scope) {
                    <call>composable()
                }
            }
        """
    )

    fun testReceiverLambda2() = assertInterceptions(
        """
            import androidx.compose.runtime.*

            class DensityScope(val density: Density)

            class Density

            val DensityCompositionLocal = CompositionLocal.of<Density>()

            @Composable
            fun compositionLocalDensity() = compositionLocal(LocalDensity)

            @Composable
            fun WithDensity(block: @Composable DensityScope.() -> Unit) {
                DensityScope(compositionLocalDensity()).<call>block()
            }
        """
    )

    fun testInlineContent() = assertInterceptions(
        """
            import androidx.compose.runtime.*
            import android.widget.LinearLayout

            @Composable fun Group(content: @Composable () -> Unit) { content() }

            @Composable
            inline fun PointerInputWrapper(
                crossinline content: @Composable () -> Unit
            ) {
                // Hide the internals of PointerInputNode
                <call>Group {
                    <call>content()
                }
            }
        """
    )

    private fun <T> setup(block: () -> T): T {
        return block()
    }

    fun assertInterceptions(srcText: String) = setup {
        val (text, carets) = extractCarets(srcText)

        val environment = myEnvironment ?: error("Environment not initialized")

        val ktFile = KtPsiFactory(environment.project).createFile(text)
        val bindingContext = JvmResolveUtil.analyze(
            ktFile,
            environment
        ).bindingContext

        carets.forEachIndexed { index, (offset, calltype) ->
            val resolvedCall = resolvedCallAtOffset(bindingContext, ktFile, offset)
                ?: error(
                    "No resolved call found at index: $index, offset: $offset. Expected " +
                        "$calltype."
                )

            when (calltype) {
                "<normal>" -> assert(!resolvedCall.isComposableInvocation())
                "<call>" -> assert(resolvedCall.isComposableInvocation())
                else -> error("Call type of $calltype not recognized.")
            }
        }
    }

    private val callPattern = Regex("(<normal>)|(<emit>)|(<call>)")
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

    private fun resolvedCallAtOffset(
        bindingContext: BindingContext,
        jetFile: KtFile,
        index: Int
    ): ResolvedCall<*>? {
        val element = jetFile.findElementAt(index)!!
        return element.getNearestResolvedCall(bindingContext)
    }
}

fun PsiElement?.getNearestResolvedCall(bindingContext: BindingContext): ResolvedCall<*>? {
    var node: PsiElement? = this
    while (node != null) {
        when (node) {
            is KtBlockExpression,
            is KtDeclaration -> return null
            is KtElement -> {
                val resolvedCall = node.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    return resolvedCall
                }
            }
        }
        node = node.parent
    }
    return null
}

private inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

private fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
    return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}