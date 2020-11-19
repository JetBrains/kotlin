/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.getJvmSignatureDiagnostics
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.highlighter.AbstractKotlinHighlightingPass.Companion.annotateDiagnostics
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

class DuplicateJvmSignatureHighlightPass(file: KtFile, document: Document) :
    AbstractBindingContextAwareHighlightingPassBase(file, document) {
    override val annotator: Annotator
        get() = DuplicateJvmSignatureAnnotator()

    inner class DuplicateJvmSignatureAnnotator : Annotator {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            if (element !is KtFile && element !is KtDeclaration) return

            val otherDiagnostics = when (element) {
                is KtDeclaration -> element.analyzeWithContent()
                is KtFile -> element.analyzeWithContent()
                else -> throw AssertionError("DuplicateJvmSignatureAnnotator: should not get here! Element: ${element.text}")
            }.diagnostics

            val moduleScope = element.getModuleInfo().contentScope()
            val diagnostics = getJvmSignatureDiagnostics(element, otherDiagnostics, moduleScope) ?: return

            val diagnosticsForElement = diagnostics.forElement(element).toSet()

            annotateDiagnostics(file, element, holder, diagnosticsForElement)
        }
    }

    class Factory : TextEditorHighlightingPassFactory {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            return if (file is KtFile &&
                ProjectRootsUtil.isInProjectSource(file) &&
                TargetPlatformDetector.getPlatform(file).isJvm()
            ) {
                DuplicateJvmSignatureHighlightPass(file, editor.document)
            } else {
                null
            }
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
                /* runAfterCompletionOf = */ intArrayOf(Pass.UPDATE_ALL),
                /* runAfterStartingOf = */ null,
                /* runIntentionsPassAfter = */ false,
                /* forcedPassId = */ -1
            )
        }
    }
}