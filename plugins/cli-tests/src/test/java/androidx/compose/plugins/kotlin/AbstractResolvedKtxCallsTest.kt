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

package androidx.compose.plugins.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import kotlin.reflect.KClass

abstract class AbstractResolvedKtxCallsTest : AbstractCodegenTest() {

    fun doTest(srcText: String, expected: String) {
        val (text, carets) = extractCarets(srcText)

        val environment = myEnvironment ?: error("Environment not initialized")

        val ktFile = KtPsiFactory(environment.project).createFile(text)
        val bindingContext = JvmResolveUtil.analyze(
            ktFile,
            environment
        ).bindingContext

        val resolvedCalls = carets.mapNotNull { caret ->
            val (element, ktxElementCall) = buildCachedCallAtIndex(bindingContext, ktFile, caret)
            val elementText = element?.text ?: error("KtxElement expected, but none found")
            val call = ktxElementCall ?: error("ResolvedKtxElementCall expected, but none found")
            elementText to call
        }

        val output = renderOutput(resolvedCalls)

        TestCase.assertEquals(expected.trimIndent(), output.trimIndent())
    }

    protected open fun renderOutput(
        resolvedCallsAt: List<Pair<String, ResolvedKtxElementCall>>
    ): String =
        resolvedCallsAt.joinToString("\n\n\n") { (_, resolvedCall) ->
            resolvedCall.print()
        }

    protected fun extractCarets(text: String): Pair<String, List<Int>> {
        val parts = text.split("<caret>")
        if (parts.size < 2) return text to emptyList()
        // possible to rewrite using 'scan' function to get partial sums of parts lengths
        val indices = mutableListOf<Int>()
        val resultText = buildString {
            parts.dropLast(1).forEach { part ->
                append(part)
                indices.add(this.length)
            }
            append(parts.last())
        }
        return resultText to indices
    }

    protected open fun buildCachedCallAtIndex(
        bindingContext: BindingContext,
        jetFile: KtFile,
        index: Int
    ): Pair<PsiElement?, ResolvedKtxElementCall?> {
        val element = jetFile.findElementAt(index)!!
        val expression = element.parentOfType<KtxElement>()

        val cachedCall = bindingContext[ComposeWritableSlices.RESOLVED_KTX_CALL, expression]
        return Pair(element, cachedCall)
    }
}
private inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

private fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
    return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}
