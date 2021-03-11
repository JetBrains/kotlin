/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.psi.KtElement

internal abstract class JKInMemoryFilesSearcher {
    abstract fun search(element: KtElement, scope: PsiElement? = null): Iterable<PsiReference>

    companion object {
        fun create(files: List<PsiElement>) = when {
            files.size == 1 -> JKSingleFileInMemoryFilesSearcher(files.single())
            else -> JKMultipleFilesInMemoryFilesSearcher(files)
        }
    }
}

internal class JKSingleFileInMemoryFilesSearcher(private val scopeElement: PsiElement) : JKInMemoryFilesSearcher() {
    override fun search(element: KtElement, scope: PsiElement?): Iterable<PsiReference> =
        ReferencesSearch.search(element, LocalSearchScope(scope ?: scopeElement))
}


// it does not cover all cases e.g, just-changed reference
// maybe the solution is to do searching manually
// firstly by-text and then resolving
internal class JKMultipleFilesInMemoryFilesSearcher(private val scopeElements: List<PsiElement>) : JKInMemoryFilesSearcher() {
    override fun search(element: KtElement, scope: PsiElement?): Iterable<PsiReference> {
        if (scope != null) {
            return ReferencesSearch.search(element, LocalSearchScope(scope))
        }
        val result = mutableListOf<PsiReference>()
        for (scopeElement in scopeElements) {
            result += ReferencesSearch.search(element, LocalSearchScope(scopeElement))
        }
        return result
    }
}
