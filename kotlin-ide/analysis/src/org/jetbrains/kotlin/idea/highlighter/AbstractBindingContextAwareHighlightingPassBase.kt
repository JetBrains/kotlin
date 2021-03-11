/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

@Suppress("UnstableApiUsage")
abstract class AbstractBindingContextAwareHighlightingPassBase(
    file: KtFile,
    document: Document
) : AbstractHighlightingPassBase(file, document) {

    private val cachedAnnotator by lazy { annotator }

    protected abstract val annotator: Annotator

    private var bindingContext: BindingContext? = null

    protected fun bindingContext(): BindingContext = bindingContext ?: error("bindingContext has to be acquired")

    protected open fun buildBindingContext(holder: AnnotationHolder): BindingContext =
        file.analyzeWithAllCompilerChecks().also { it.throwIfError() }.bindingContext

    override fun runAnnotatorWithContext(element: PsiElement, holder: AnnotationHolder) {
        bindingContext = buildBindingContext(holder)
        try {
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    cachedAnnotator.annotate(element, holder)
                    super.visitElement(element)
                }
            })
        } finally {
            bindingContext = null
        }
    }
}