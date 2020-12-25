/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.checkers.utils.DebugInfoUtil
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression

/**
 * Quick showing possible problems with Kotlin internals in IDEA with tooltips
 */
class DebugInfoAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!isDebugInfoEnabled || !ProjectRootsUtil.isInProjectOrLibSource(element)) {
            return
        }

        if (element is KtFile && element !is KtCodeFragment) {
            try {
                val bindingContext = element.analyzeWithContent()
                DebugInfoUtil.markDebugAnnotations(element, bindingContext, object : DebugInfoUtil.DebugInfoReporter() {
                    override fun reportElementWithErrorType(expression: KtReferenceExpression) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Resolved to error element").textAttributes =
                            KotlinHighlightingColors.RESOLVED_TO_ERROR
                    }

                    override fun reportMissingUnresolved(expression: KtReferenceExpression) {
                        holder.createErrorAnnotation(
                            expression,
                            "[DEBUG] Reference is not resolved to anything, but is not marked unresolved"
                        ).textAttributes = KotlinHighlightingColors.DEBUG_INFO
                    }

                    override fun reportUnresolvedWithTarget(expression: KtReferenceExpression, target: String) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Reference marked as unresolved is actually resolved to $target")
                            .textAttributes = KotlinHighlightingColors.DEBUG_INFO
                    }
                })
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                // TODO
                holder.createErrorAnnotation(element, e.javaClass.canonicalName + ": " + e.message)
                e.printStackTrace()
            }

        }
    }

    companion object {
        val isDebugInfoEnabled: Boolean
            get() = ApplicationManager.getApplication().isUnitTestMode ||
                    (ApplicationManager.getApplication().isInternal &&
                            (KotlinPluginUtil.isSnapshotVersion() || KotlinPluginUtil.isDevVersion()))
    }
}
