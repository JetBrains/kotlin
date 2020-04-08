/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

class KotlinPropertyCallUsage(element: KtSimpleNameExpression) : KotlinUsageInfo<KtSimpleNameExpression>(element) {
    private val resolvedCall = element.resolveToCall(BodyResolveMode.FULL)

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression, allUsages: Array<out UsageInfo>): Boolean {
        updateName(changeInfo, element)
        updateReceiver(changeInfo, element)
        return true
    }

    private fun updateName(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression) {
        if (changeInfo.isNameChanged) {
            element.mainReference.handleElementRename(changeInfo.newName)
        }
    }

    private fun updateReceiver(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression) {
        val newReceiver = changeInfo.receiverParameterInfo
        val oldReceiver = changeInfo.methodDescriptor.receiver
        if (newReceiver == oldReceiver) return

        val elementToReplace = element.getQualifiedExpressionForSelectorOrThis()

        // Do not add extension receiver to calls with explicit dispatch receiver
        if (newReceiver != null
            && elementToReplace is KtQualifiedExpression
            && resolvedCall?.dispatchReceiver is ExpressionReceiver
        ) return

        val replacingElement = newReceiver?.let {
            val psiFactory = KtPsiFactory(project)
            val receiver = it.defaultValueForCall ?: psiFactory.createExpression("_")
            psiFactory.createExpressionByPattern("$0.$1", receiver, element)
        } ?: element

        elementToReplace.replace(replacingElement)
    }
}