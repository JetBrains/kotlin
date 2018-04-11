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

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.navigation.GotoRelatedItem
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.spring.gutter.SpringClassAnnotator
import com.intellij.util.Function
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import javax.swing.Icon

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
        psiElement.getParentOfTypeAndBranch<KtConstructor<*>> { getConstructorKeyword() ?: getValueParameterList() }?.let {
            return it.toLightMethods().firstOrNull()
        }
        psiElement.getParentOfTypeAndBranch<KtProperty> { nameIdentifier }?.let { return it }
        psiElement.getParentOfTypeAndBranch<KtParameter> { nameIdentifier }?.let { if (it.valOrVarKeyword != null) return it }
        psiElement.getParentOfTypeAndBranch<KtAnnotationEntry> {
            (typeReference?.typeElement as? KtUserType)?.referenceExpression?.getReferencedNameElement()
        }?.let { return it.toLightAnnotation() }
        return null
    }

    private fun doCollectMarkers(psiElement: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        if (psiElement is KtProperty || psiElement is KtParameter) {
            for (it in (psiElement as KtDeclaration).toLightElements()) {
                val nameIdentifier = (it as? PsiNameIdentifierOwner)?.nameIdentifier ?: continue
                super.collectNavigationMarkers(nameIdentifier, result)
            }
            return
        }

        // Workaround for SpringClassAnnotator
        (getElementToProcess(psiElement) as? KtLightAnnotationForSourceEntry)?.let { return super.collectNavigationMarkers(it, result) }

        super.collectNavigationMarkers(psiElement, result)
    }

    // TODO
    // Weak references to light elements may be reclaimed by GC after original file is modified causing line markers to misbehave
    // This workaround allows reuse of SpringClassAnnotator logic and avoids binding of line markers to light elements

    private val toolTipProviderField by lazy { LineMarkerInfo::class.java.getDeclaredField("myTooltipProvider").apply { isAccessible = true } }
    private val iconField by lazy { LineMarkerInfo::class.java.getDeclaredField("myIcon").apply { isAccessible = true } }
    private val iconAlignmentField by lazy { LineMarkerInfo::class.java.getDeclaredField("myIconAlignment").apply { isAccessible = true } }
    private val targetsField by lazy { RelatedItemLineMarkerInfo::class.java.getDeclaredField("myTargets").apply { isAccessible = true } }

    override fun collectNavigationMarkers(psiElement: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        val newItems = SmartList<RelatedItemLineMarkerInfo<PsiElement>>()

        doCollectMarkers(psiElement, newItems)

        newItems.mapNotNullTo(result) { item ->
            val itemElement = item.element
            val elementToAnnotate = when (itemElement) {
                is KtLightIdentifier -> itemElement.origin
                is KtLightElement<*, *> -> itemElement.kotlinOrigin
                else -> return@mapNotNullTo item
            }
            if (elementToAnnotate == null) return@mapNotNullTo null
            if (alreadyMarked(result, elementToAnnotate, item.navigationHandler)) return@mapNotNullTo null

            @Suppress("UNCHECKED_CAST")
            RelatedItemLineMarkerInfo<PsiElement>(
                    elementToAnnotate,
                    elementToAnnotate.textRange,
                    iconField.get(item) as Icon?,
                    item.updatePass,
                    toolTipProviderField.get(item) as Function<PsiElement, String>,
                    item.navigationHandler,
                    iconAlignmentField.get(item) as GutterIconRenderer.Alignment,
                    targetsField.get(item) as NotNullLazyValue<Collection<GotoRelatedItem>>
            )
        }
    }

    private fun alreadyMarked(result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>,
                              elementToAnnotate: PsiElement,
                              navigationHandler: GutterIconNavigationHandler<*>?) =
            result.any {
                when (it) {
                    is RelatedItemLineMarkerInfo<*> -> {
                        it.element == elementToAnnotate && it.navigationHandler == navigationHandler
                    }
                    else -> false
                }
            }
}