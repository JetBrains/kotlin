package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.ast.ResolvedKtxElementCall
import org.jetbrains.kotlin.r4a.ast.print
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil

abstract class AbstractResolvedKtxCallsTest : AbstractCodegenTest() {

    fun doTest(srcText: String, expected: String) {
        val (text, carets) = extractCarets(srcText)

        val environment = myEnvironment ?: error("Environment not initialized")
        setupLanguageVersionSettingsForCompilerTests(srcText, environment)

        val ktFile = KtPsiFactory(environment.project).createFile(text)
        val bindingContext = JvmResolveUtil.analyze(ktFile, environment).bindingContext

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

        val cachedCall = bindingContext[R4AWritableSlices.RESOLVED_KTX_CALL, expression]
        return Pair(element, cachedCall)
    }
}