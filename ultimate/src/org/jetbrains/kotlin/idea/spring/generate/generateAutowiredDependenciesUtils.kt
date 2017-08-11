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

package org.jetbrains.kotlin.idea.spring.generate

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.spring.CommonSpringModel
import com.intellij.spring.constants.SpringAnnotationsConstants
import com.intellij.spring.model.CommonSpringBean
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.model.SpringModelSearchParameters
import com.intellij.spring.model.actions.generate.GenerateSpringBeanDependenciesUtil
import com.intellij.spring.model.utils.SpringModelSearchers
import com.intellij.spring.model.utils.SpringModelUtils
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.editor.BatchTemplateRunner
import org.jetbrains.kotlin.idea.spring.effectiveBeanClasses
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.utils.ifEmpty

// TODO: GenerateAutowiredDependenciesUtil.getQualifierName() is not accessible here
private fun SpringBeanPointer<CommonSpringBean>.getQualifierName(): String? {
    val value = springBean.springQualifier?.qualifierValue
    return if (value.isNullOrBlank()) name else value
}

private fun createAutowiredProperty(
        currentBeanClass: KtLightClass,
        candidateBean: SpringBeanPointer<CommonSpringBean>,
        candidateBeanClasses: Array<out PsiClass>,
        model: CommonSpringModel
): KtProperty? {
    try {
        val ktBeanClass = currentBeanClass.kotlinOrigin ?: return null

        val qualifierName = candidateBean.getQualifierName()
        val candidateBeanClass = candidateBeanClasses.first()
        val beanName = candidateBean.name
        val name = if (beanName != null && KotlinNameSuggester.isIdentifier(beanName)) beanName else candidateBeanClass.name!!

        val psiFactory = KtPsiFactory(currentBeanClass.project)
        // TODO: Use KtPsiFactory.CallableBuilder
        val prototype = psiFactory.createProperty("lateinit var ${name.decapitalize()}: ${candidateBeanClass.defaultTypeText}").apply {
            addAnnotationEntry(psiFactory.createAnnotationEntry("@${SpringAnnotationsConstants.AUTOWIRED}"))

            val searchParameters = SpringModelSearchParameters.byClass(candidateBeanClass).withInheritors().effectiveBeanTypes()
            if (SpringModelSearchers.findBeans(model, searchParameters).size > 1 && !qualifierName.isNullOrBlank()) {
                val annotationText = "@${SpringAnnotationsConstants.QUALIFIER}(\"${StringUtil.escapeStringCharacters(qualifierName!!)}\")"
                addAnnotationEntry(psiFactory.createAnnotationEntry(annotationText))
            }
        }
        return ktBeanClass.addDeclaration(prototype).apply { ShortenReferences.DEFAULT.process(this) }
    }
    catch (e: IncorrectOperationException) {
        throw RuntimeException(e)
    }
}

private fun addCreatePropertyTemplate(
        property: KtProperty,
        candidateBean: SpringBeanPointer<CommonSpringBean>,
        candidateBeanClasses: Array<out PsiClass>
): BatchTemplateRunner {
    return BatchTemplateRunner(property.project).apply {
        val propertyPointer = property.createSmartPointer()
        addTemplateFactory(property) {
            val currentProperty = propertyPointer.element ?: return@addTemplateFactory null
            val builder = TemplateBuilderImpl(currentProperty)
            builder.appendVariableTemplate(currentProperty, candidateBeanClasses) {
                val existingNames = currentProperty.containingClassOrObject!!.declarations
                        .mapNotNull { if (it != currentProperty) (it as? KtProperty)?.name else null }
                getSuggestedNames(candidateBean, currentProperty, existingNames = existingNames) { returnType }
            }
            builder.buildInlineTemplate()
        }
    }
}

private fun createAutowiredDependency(
        klass: KtLightClass,
        candidateBean: SpringBeanPointer<CommonSpringBean>,
        model: CommonSpringModel
): BatchTemplateRunner? {
    val candidateBeanClasses = candidateBean.effectiveBeanClasses().ifEmpty { return null }
    if (!GenerateSpringBeanDependenciesUtil.ensureFileWritable(klass)) return null
    val property = createAutowiredProperty(klass, candidateBean, candidateBeanClasses, model) ?: return null
    return addCreatePropertyTemplate(property, candidateBean, candidateBeanClasses)
}

fun generateAutowiredDependenciesFor(klass: KtLightClass): List<BatchTemplateRunner> {
    val model = SpringModelUtils.getInstance().getPsiClassSpringModel(klass)
    val candidates = GenerateSpringBeanDependenciesUtil.getAutowiredBeanCandidates(model) { true }
    val dependencies = if (ApplicationManager.getApplication().isUnitTestMode) {
        candidates.map { it.springBean }.filter(klass.project.beanFilter).sortedBy { it.name }
    } else {
        GenerateSpringBeanDependenciesUtil.chooseDependentBeans(candidates, klass.project, true)
    }
    return dependencies.mapNotNull { createAutowiredDependency(klass, it, model) }
}