/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

abstract class AbstractDslHighlighterTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(unused: String) {
        val psiFile = myFixture.configureByFile(fileName()) as KtFile
        val extension = DslHighlighterExtension()
        val bindingContext = psiFile.analyzeWithAllCompilerChecks().bindingContext

        fun checkCall(element: KtElement) {
            val call = element.getResolvedCall(bindingContext) ?: return
            val lineNumber = editor.document.getLineNumber(element.textOffset)
            val endOffset = editor.document.getLineEndOffset(lineNumber)
            val commentText = (file.findElementAt(endOffset - 1) as? PsiComment)?.text
            val styleIdByComment = commentText?.replace("//", "")?.trim()?.toInt()?.let { DslHighlighterExtension.externalKeyName(it) }
            val styleIdByCall = extension.highlightCall(element, call)?.externalName
            if (styleIdByCall != null && styleIdByCall == styleIdByComment) {
                val annotationHolder = AnnotationHolderImpl(AnnotationSession(psiFile))
                val checkers = KotlinPsiChecker.getAfterAnalysisVisitor(annotationHolder, bindingContext)
                checkers.forEach { call.call.callElement.accept(it) }
                assertTrue(
                    "KotlinPsiChecker did not contribute an Annotation containing the correct text attribute key at line ${lineNumber + 1}",
                    annotationHolder.any {
                        it.textAttributes.externalName == styleIdByComment
                    }
                )
            } else if (styleIdByCall != styleIdByComment) {
                val what = element.text
                val location = "at line ${editor.document.getLineNumber(element.textOffset) + 1}"

                if (styleIdByCall == null) fail("Expected `$what` to be highlighted $location")
                if (styleIdByComment == null) fail("Unexpected highlighting of `$what` $location")

                fail("Expected: $styleIdByComment, got: $styleIdByCall for $what $location")
            }

        }

        val visitor = object : KtTreeVisitor<Unit?>() {
            override fun visitKtElement(element: KtElement, data: Unit?): Void? {
                checkCall(element)
                return super.visitKtElement(element, data)
            }
        }

        psiFile.accept(visitor)
    }

}