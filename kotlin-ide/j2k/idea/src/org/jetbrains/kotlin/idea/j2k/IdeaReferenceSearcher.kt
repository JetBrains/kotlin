/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import java.util.*

object IdeaReferenceSearcher : ReferenceSearcher {
    override fun findLocalUsages(element: PsiElement, scope: PsiElement) =
        ReferencesSearch.search(element, LocalSearchScope(scope)).findAll()

    override fun hasInheritors(`class`: PsiClass) = ClassInheritorsSearch.search(`class`, false).any()

    override fun hasOverrides(method: PsiMethod) = OverridingMethodsSearch.search(method, false).any()

    override fun findUsagesForExternalCodeProcessing(
        element: PsiElement,
        searchJava: Boolean,
        searchKotlin: Boolean
    ): Collection<PsiReference> {
        val fileTypes = ArrayList<FileType>()
        if (searchJava) {
            fileTypes.add(JavaLanguage.INSTANCE.associatedFileType!!)
        }
        if (searchKotlin) {
            fileTypes.add(KotlinLanguage.INSTANCE.associatedFileType!!)
        }
        val searchScope =
            GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(element.project), *fileTypes.toTypedArray())
        return ReferencesSearch.search(element, searchScope).findAll()
    }
}
