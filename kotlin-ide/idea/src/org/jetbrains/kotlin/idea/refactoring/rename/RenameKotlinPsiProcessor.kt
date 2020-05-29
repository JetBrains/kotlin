/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters

// FIX ME WHEN BUNCH 191 REMOVED
abstract class RenameKotlinPsiProcessor : RenameKotlinPsiProcessorCompat() {

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        val searchParameters = KotlinReferencesSearchParameters(
            element,
            searchScope,
            kotlinOptions = KotlinReferencesSearchOptions(
                searchForComponentConventions = false,
                acceptImportAlias = false
            )
        )
        return findReferences(element, searchParameters)
    }

}
