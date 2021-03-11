/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.daemon.RainbowVisitor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinTargetElementEvaluator
import org.jetbrains.kotlin.idea.util.isAnonymousFunction
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinRainbowVisitor : RainbowVisitor() {
    companion object {
        val KOTLIN_TARGET_ELEMENT_EVALUATOR = KotlinTargetElementEvaluator()
    }

    override fun suitableForFile(file: PsiFile) = file is KtFile

    override fun clone() = KotlinRainbowVisitor()

    override fun visit(element: PsiElement) {
        when {
            element.isRainbowDeclaration() -> {
                val rainbowElement = (element as KtNamedDeclaration).nameIdentifier ?: return
                addRainbowHighlight(element, rainbowElement)
            }

            element is KtSimpleNameExpression -> {
                val qualifiedExpression = PsiTreeUtil.getParentOfType(
                    element, KtQualifiedExpression::class.java, true,
                    KtLambdaExpression::class.java, KtValueArgumentList::class.java
                )
                if (qualifiedExpression?.selectorExpression?.isAncestor(element) == true) return

                val reference = element.mainReference
                val targetElement = reference.resolve()
                if (targetElement != null) {
                    if (targetElement.isRainbowDeclaration()) {
                        addRainbowHighlight(targetElement, element)
                    }
                } else if (element.getReferencedName() == "it") {
                    val itTargetElement =
                        KOTLIN_TARGET_ELEMENT_EVALUATOR.getElementByReference(reference, TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED)

                    if (itTargetElement != null) {
                        addRainbowHighlight(itTargetElement, element)
                    }
                }
            }

            element is KDocName -> {
                val target = element.reference?.resolve() ?: return
                if (target.isRainbowDeclaration()) {
                    addRainbowHighlight(target, element, KDOC_LINK)
                }
            }
        }
    }

    private fun addRainbowHighlight(target: PsiElement, rainbowElement: PsiElement, attributesKey: TextAttributesKey? = null) {
        val lambdaSequenceIterator = target.parents
            .takeWhile { it !is KtDeclaration || it.isAnonymousFunction || it is KtFunctionLiteral }
            .filter { it is KtLambdaExpression || it.isAnonymousFunction }
            .iterator()

        val attributesKeyToUse = attributesKey ?: (if (target is KtParameter) PARAMETER else LOCAL_VARIABLE)
        if (lambdaSequenceIterator.hasNext()) {
            var lambda = lambdaSequenceIterator.next()
            var lambdaNestingLevel = 0
            while (lambdaSequenceIterator.hasNext()) {
                lambdaNestingLevel++
                lambda = lambdaSequenceIterator.next()
            }

            addInfo(getInfo(lambda, rainbowElement, "$lambdaNestingLevel${rainbowElement.text}", attributesKeyToUse))
            return
        }

        val context = target.getStrictParentOfType<KtDeclarationWithBody>()
            ?: target.getStrictParentOfType<KtAnonymousInitializer>()
            ?: return

        addInfo(getInfo(context, rainbowElement, rainbowElement.text, attributesKeyToUse))
    }

    private fun PsiElement.isRainbowDeclaration(): Boolean =
        (this is KtProperty && isLocal) ||
                (this is KtParameter && getStrictParentOfType<KtPrimaryConstructor>() == null) ||
                this is KtDestructuringDeclarationEntry
}
