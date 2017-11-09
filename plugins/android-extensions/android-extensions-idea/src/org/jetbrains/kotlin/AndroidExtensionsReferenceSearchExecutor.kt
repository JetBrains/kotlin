/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.Processor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi


class AndroidExtensionsReferenceSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val elementToSearch = queryParameters.elementToSearch as? XmlAttributeValue ?: return
        val scopeElements = (queryParameters.effectiveSearchScope as? LocalSearchScope)?.scope ?: return
        val referenceName = elementToSearch.value?.substringAfterLast("/") ?: return

        scopeElements.filterIsInstance<KtElement>().forEach {
            it.accept(object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (expression.text == referenceName && expression.isReferenceTo(elementToSearch)) {
                        expression.references.firstOrNull { it is KtSimpleNameReference }?.let {
                            consumer.process(it)
                        }
                    }
                    super.visitReferenceExpression(expression)
                }
            })
        }
    }

    private fun KtReferenceExpression.isReferenceTo(element: PsiElement): Boolean =
            getTargetPropertyDescriptor()?.source?.getPsi() == element

    private fun KtReferenceExpression.getTargetPropertyDescriptor(): PropertyDescriptor? =
            analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, this] as? PropertyDescriptor
}