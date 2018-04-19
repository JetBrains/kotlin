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

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.*
import com.intellij.spring.model.DefaultSpringBeanQualifier
import com.intellij.spring.model.converters.SpringConverterUtil
import com.intellij.spring.model.highlighting.SpringAutowireUtil
import com.intellij.spring.model.jam.qualifiers.SpringJamQualifier
import com.intellij.spring.model.utils.SpringModelSearchers
import com.intellij.spring.references.SpringQualifierReference
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.spring.springModel
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

// Based on com.intellij.spring.references.SpringQualifierReference
// It inherits from SpringQualifierReference to allow reuse of existing Java inspections which explicitly check for SpringQualifierReference
class KtSpringQualifierReference(element: KtStringTemplateExpression) : SpringQualifierReference(StringTemplatePsiLiteralWrapper(element)) {
    class StringTemplatePsiLiteralWrapper(
            val element: KtStringTemplateExpression
    ) : PsiElement by element, PsiLiteralExpression, KtLightElement<KtStringTemplateExpression, KtStringTemplateExpression>, PsiCompiledElement {

        override fun getValue() = element.plainContent
        override fun getType() = PsiType.getJavaLangString(element.manager, element.project.allScope())

        override val clsDelegate: KtStringTemplateExpression
            get() = element
        override val kotlinOrigin: KtStringTemplateExpression
            get() = element

        override fun getMirror() = null
    }

    private fun getAnnotationFqName(): String? {
        val annotationEntry = element.getStrictParentOfType<KtAnnotationEntry>() ?: return null
        val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
        val annotationType = context[BindingContext.TYPE, annotationEntry.typeReference] ?: return null
        return annotationType.constructor.declarationDescriptor?.importableFqName?.asString()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val annotationEntry = element.getStrictParentOfType<KtAnnotationEntry>() ?: return ResolveResult.EMPTY_ARRAY
        val lightAnnotation = annotationEntry.toLightAnnotation() ?: return ResolveResult.EMPTY_ARRAY
        val springModel = element.springModel ?: return ResolveResult.EMPTY_ARRAY

        val results = ArrayList<ResolveResult>()
        val jamQualifier = SpringJamQualifier(lightAnnotation, null)

        val qualifiedBeans = springModel.findQualifiedBeans(jamQualifier)
        qualifiedBeans.mapTo(results) { PsiElementResolveResult(it.springBean.springQualifier!!.identifyingPsiElement) }

        val qualifierValue = jamQualifier.qualifierValue
        if (qualifierValue != null) {
            val beanPointer = SpringModelSearchers.findBean(springModel, qualifierValue)
            val psiElement = if (beanPointer != null && beanPointer.isValid) beanPointer.psiElement else null
            if (psiElement != null) results += PsiElementResolveResult(psiElement)
        }

        return results.toTypedArray()
    }

    // We can't get light elements for property in the synthetic completion KtFile, so look for original one instead
    private fun getOriginalProperty(): KtProperty? {
        val currentProperty = element.getStrictParentOfType<KtProperty>() ?: return null
        val originalFile = element.containingFile.originalFile as? KtFile ?: return null
        return originalFile.findElementAt(currentProperty.startOffset)?.getNonStrictParentOfType<KtProperty>()
    }

    override fun getVariants(): Array<out Any> {
        val annotationFqName = getAnnotationFqName() ?: return LookupElement.EMPTY_ARRAY
        val springModel = element.springModel ?: return LookupElement.EMPTY_ARRAY
        val property = getOriginalProperty() ?: return LookupElement.EMPTY_ARRAY
        val lightElement = property.toLightElements().firstOrNull {
            it is PsiField || (it is PsiMethod && !it.name.startsWith("set"))
        }
        val expectedPsiType = when (lightElement) {
                                  is PsiMethod -> lightElement.returnType
                                  is PsiField -> lightElement.type
                                  else -> null
                              } ?: return LookupElement.EMPTY_ARRAY
        return SpringAutowireUtil
                .autowireByType(springModel, expectedPsiType)
                .mapNotNull {
                    val qualifier = it.springBean.springQualifier
                    val value = qualifier?.qualifierValue
                    when {
                        qualifier == null ->
                            SpringConverterUtil.createCompletionVariant(it)
                        value != null && (qualifier is DefaultSpringBeanQualifier || qualifier.qualifierType?.qualifiedName == annotationFqName) ->
                            SpringConverterUtil.createCompletionVariant(it, value)
                        else ->
                            null
                    }
                }
                .toTypedArray()
    }
}