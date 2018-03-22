/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.psi.PsiComment
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

abstract class AbstractDslHighlighterTest : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(filePath: String) {
        val psiFile = myFixture.configureByFile(filePath) as KtFile
        val extension = DslHighlighterExtension()
        val bindingContext = psiFile.analyzeFullyAndGetResult().bindingContext

        fun checkCall(element: KtElement) {
            val call = element.getResolvedCall(bindingContext) ?: return
            val lineNumber = editor.document.getLineNumber(element.textOffset)
            val endOffset = editor.document.getLineEndOffset(lineNumber)
            val commentText = (file.findElementAt(endOffset - 1) as? PsiComment)?.text
            val styleIdByComment = commentText?.replace("//", "")?.trim()?.toInt()?.let { DslHighlighterExtension.externalKeyName(it) }
            val styleIdByCall = extension.highlightCall(element, call)?.externalName
            if (styleIdByCall == styleIdByComment) return

            val what = element.text
            val location = "at line ${editor.document.getLineNumber(element.textOffset) + 1}"

            if (styleIdByCall == null) fail("Expected `$what` to be highlighted $location")
            if (styleIdByComment == null) fail("Unexpected highlighting of `$what` $location")

            fail("Expected: $styleIdByComment, got: $styleIdByCall for $what $location")
        }

        val visitor = object : KtTreeVisitor<Unit?>() {
            override fun visitKtElement(element: KtElement, data: Unit?): Void? {
                checkCall(element)
                return super.visitKtElement(element, data)
            }
        }

        psiFile.accept(visitor)
    }

    override fun getTestDataPath() = ""
}