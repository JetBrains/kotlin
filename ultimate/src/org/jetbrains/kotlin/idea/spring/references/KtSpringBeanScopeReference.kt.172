/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.references

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.TypePresentationService
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiReferenceBase
import com.intellij.spring.model.scope.SpringBeanScope
import com.intellij.spring.model.scope.SpringBeanScopeManager
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent

class KtSpringBeanScopeReference(
        element: KtStringTemplateExpression
) : PsiReferenceBase<KtStringTemplateExpression>(element, ElementManipulators.getManipulator(element).getRangeInElement(element)) {
    private fun getScopes(): Sequence<SpringBeanScope> {
        return SpringBeanScope.getDefaultScopes().asSequence() + SpringBeanScopeManager.getInstance().getCustomBeanScopes(element)
    }

    private fun getLookupElement(scope: String): LookupElementBuilder {
        return LookupElementBuilder
                .create(scope)
                .withIcon(TypePresentationService.getService().getTypeIcon(SpringBeanScope::class.java))
    }

    override fun resolve() = if (element.plainContent in getScopes().map { it.value }) element else null

    override fun getVariants() = getScopes().map { getLookupElement(it.value) }.toList().toTypedArray()
}
