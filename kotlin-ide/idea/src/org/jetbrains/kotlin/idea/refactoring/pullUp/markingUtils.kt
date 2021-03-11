/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import java.util.*

private var KtElement.newFqName: FqName? by CopyablePsiUserDataProperty(Key.create("NEW_FQ_NAME"))
private var KtElement.replaceWithTargetThis: Boolean? by CopyablePsiUserDataProperty(Key.create("REPLACE_WITH_TARGET_THIS"))
private var KtElement.newTypeText: ((TypeSubstitutor) -> String?)? by CopyablePsiUserDataProperty(Key.create("NEW_TYPE_TEXT"))

fun markElements(
    declaration: KtNamedDeclaration,
    context: BindingContext,
    sourceClassDescriptor: ClassDescriptor, targetClassDescriptor: ClassDescriptor?
): List<KtElement> {
    val affectedElements = ArrayList<KtElement>()

    declaration.accept(
        object : KtVisitorVoid() {
            private fun visitSuperOrThis(expression: KtInstanceExpressionWithLabel) {
                if (targetClassDescriptor == null) return

                val callee = expression.getQualifiedExpressionForReceiver()?.selectorExpression?.getCalleeExpressionIfAny() ?: return
                val calleeTarget = callee.getResolvedCall(context)?.resultingDescriptor ?: return
                if ((calleeTarget as? CallableMemberDescriptor)?.kind != CallableMemberDescriptor.Kind.DECLARATION) return
                if (calleeTarget.containingDeclaration == targetClassDescriptor) {
                    expression.replaceWithTargetThis = true
                    affectedElements.add(expression)
                }
            }

            override fun visitElement(element: PsiElement) {
                element.allChildren.forEach { it.accept(this) }
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val receiver = resolvedCall.getExplicitReceiverValue()
                    ?: resolvedCall.extensionReceiver
                    ?: resolvedCall.dispatchReceiver
                    ?: return

                val implicitThis = receiver.type.constructor.declarationDescriptor as? ClassDescriptor ?: return
                if (implicitThis.isCompanionObject
                    && DescriptorUtils.isAncestor(sourceClassDescriptor, implicitThis, true)
                ) {
                    val qualifierFqName = implicitThis.importableFqName ?: return

                    expression.newFqName = FqName("${qualifierFqName.asString()}.${expression.getReferencedName()}")
                    affectedElements.add(expression)
                }
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                visitSuperOrThis(expression)
            }

            override fun visitSuperExpression(expression: KtSuperExpression) {
                visitSuperOrThis(expression)
            }

            override fun visitTypeReference(typeReference: KtTypeReference) {
                val oldType = context[BindingContext.TYPE, typeReference] ?: return
                typeReference.newTypeText = f@{ substitutor ->
                    substitutor.substitute(oldType, Variance.INVARIANT)?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
                }
                affectedElements.add(typeReference)
            }
        }
    )

    return affectedElements
}

fun applyMarking(
    declaration: KtNamedDeclaration,
    substitutor: TypeSubstitutor, targetClassDescriptor: ClassDescriptor
) {
    val psiFactory = KtPsiFactory(declaration)
    val targetThis = psiFactory.createExpression("this@${targetClassDescriptor.name.asString().quoteIfNeeded()}")
    val shorteningOptionsForThis = ShortenReferences.Options(removeThisLabels = true, removeThis = true)

    declaration.accept(
        object : KtVisitorVoid() {
            private fun visitSuperOrThis(expression: KtInstanceExpressionWithLabel) {
                expression.replaceWithTargetThis?.let {
                    expression.replaceWithTargetThis = null

                    val newThisExpression = expression.replace(targetThis) as KtExpression
                    newThisExpression.getQualifiedExpressionForReceiverOrThis().addToShorteningWaitSet(shorteningOptionsForThis)
                }
            }

            override fun visitElement(element: PsiElement) {
                for (it in element.allChildren.toList()) {
                    it.accept(this)
                }
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                expression.newFqName?.let {
                    expression.newFqName = null

                    expression.mainReference.bindToFqName(it)
                }
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                this.visitSuperOrThis(expression)
            }

            override fun visitSuperExpression(expression: KtSuperExpression) {
                this.visitSuperOrThis(expression)
            }

            override fun visitTypeReference(typeReference: KtTypeReference) {
                typeReference.newTypeText?.let f@{
                    typeReference.newTypeText = null

                    val newTypeText = it(substitutor) ?: return@f
                    (typeReference.replace(psiFactory.createType(newTypeText)) as KtElement).addToShorteningWaitSet()
                }
            }
        }
    )
}

fun clearMarking(markedElements: List<KtElement>) {
    markedElements.forEach {
        it.newFqName = null
        it.newTypeText = null
        it.replaceWithTargetThis = null
    }
}