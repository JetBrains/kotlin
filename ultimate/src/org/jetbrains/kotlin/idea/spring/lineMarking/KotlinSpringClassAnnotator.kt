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

package org.jetbrains.kotlin.idea.spring.lineMarking

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.spring.gutter.SpringClassAnnotator
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation

class KotlinSpringClassAnnotator : SpringClassAnnotator() {
    override fun getElementToProcess(psiElement: PsiElement): PsiElement? {
        if (psiElement is KtLightIdentifier) return psiElement.parent
        psiElement.getParentOfTypeAndBranch<KtClass> { nameIdentifier }?.let { return it.toLightClass() }
        psiElement.getParentOfTypeAndBranch<KtNamedFunction> { nameIdentifier }?.let { function ->
            val containingClassOrObject = function.containingClassOrObject
            val classForLightMethod = if (containingClassOrObject is KtObjectDeclaration
                                          && containingClassOrObject.isCompanion()
                                          && function.resolveToDescriptor().hasJvmStaticAnnotation()) {
                containingClassOrObject.containingClassOrObject
            }
            else {
                containingClassOrObject
            }
            return classForLightMethod?.toLightClass()?.methods?.firstOrNull { (it as? KtLightMethod)?.kotlinOrigin == function }
        }
        psiElement.getParentOfTypeAndBranch<KtProperty> { nameIdentifier }?.let { return it }
        psiElement.getParentOfTypeAndBranch<KtAnnotationEntry> {
            (typeReference?.typeElement as? KtUserType)?.referenceExpression?.getReferencedNameElement()
        }?.let { return it.toLightAnnotation() }
        return null
    }

    override fun collectNavigationMarkers(psiElement: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>?) {
        if (psiElement is KtProperty) {
            for (it in psiElement.toLightElements()) {
                val nameIdentifier = (it as? PsiNameIdentifierOwner)?.nameIdentifier ?: continue
                super.collectNavigationMarkers(nameIdentifier, result)
            }
            return
        }

        // Workaround for SpringClassAnnotator
        (getElementToProcess(psiElement) as? KtLightAnnotation)?.let { return super.collectNavigationMarkers(it, result) }

        super.collectNavigationMarkers(psiElement, result)
    }
}