/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.JetLanguage
import java.util.ArrayList

public trait ReferenceSearcher {
    fun findLocalUsages(element: PsiElement, scope: PsiElement): Collection<PsiReference>
    fun hasInheritors(`class`: PsiClass): Boolean
    fun hasOverrides(method: PsiMethod): Boolean

    fun findUsagesForExternalCodeProcessing(element: PsiElement, searchJava: Boolean, searchKotlin: Boolean): Collection<PsiReference>
}

public fun ReferenceSearcher.findVariableUsages(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression>
        = findLocalUsages(variable, scope).filterIsInstance<PsiReferenceExpression>()

public fun ReferenceSearcher.findMethodCalls(method: PsiMethod, scope: PsiElement): Collection<PsiMethodCallExpression> {
    return findLocalUsages(method, scope).map {
        if (it is PsiReferenceExpression) {
            val methodCall = it.getParent() as? PsiMethodCallExpression
            if (methodCall?.getMethodExpression() == it) methodCall else null
        }
        else {
            null
        }
    }.filterNotNull()
}

public object EmptyReferenceSearcher: ReferenceSearcher {
    override fun findLocalUsages(element: PsiElement, scope: PsiElement): Collection<PsiReference> = emptyList()
    override fun hasInheritors(`class`: PsiClass) = false
    override fun hasOverrides(method: PsiMethod) = false
    override fun findUsagesForExternalCodeProcessing(element: PsiElement, searchJava: Boolean, searchKotlin: Boolean): Collection<PsiReference>
            = throw UnsupportedOperationException()
}

public object IdeaReferenceSearcher: ReferenceSearcher {
    override fun findLocalUsages(element: PsiElement, scope: PsiElement) = ReferencesSearch.search(element, LocalSearchScope(scope)).findAll()

    override fun hasInheritors(`class`: PsiClass) = ClassInheritorsSearch.search(`class`, false).any()

    override fun hasOverrides(method: PsiMethod) = OverridingMethodsSearch.search(method, false).any()

    override fun findUsagesForExternalCodeProcessing(element: PsiElement, searchJava: Boolean, searchKotlin: Boolean): Collection<PsiReference> {
        val fileTypes = ArrayList<FileType>()
        if (searchJava) {
            fileTypes.add(JavaLanguage.INSTANCE.getAssociatedFileType())
        }
        if (searchKotlin) {
            fileTypes.add(JetLanguage.INSTANCE.getAssociatedFileType())
        }
        val searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(element.getProject()), *fileTypes.toTypedArray())
        return ReferencesSearch.search(element, searchScope).findAll()
    }
}
