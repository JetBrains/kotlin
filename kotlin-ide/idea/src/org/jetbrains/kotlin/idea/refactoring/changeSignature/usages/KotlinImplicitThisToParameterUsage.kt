/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.ShortenReferences.Options
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils

abstract class KotlinImplicitReceiverUsage(callElement: KtElement) : KotlinUsageInfo<KtElement>(callElement) {
    protected abstract fun getNewReceiverText(): String

    protected open fun processReplacedElement(element: KtElement) {

    }

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtElement, allUsages: Array<out UsageInfo>): Boolean {
        val newQualifiedCall = KtPsiFactory(element.project).createExpression(
            "${getNewReceiverText()}.${element.text}"
        ) as KtQualifiedExpression
        processReplacedElement(element.replace(newQualifiedCall) as KtElement)
        return false
    }
}

class KotlinImplicitThisToParameterUsage(
    callElement: KtElement,
    val parameterInfo: KotlinParameterInfo,
    val containingCallable: KotlinCallableDefinitionUsage<*>
) : KotlinImplicitReceiverUsage(callElement) {
    override fun getNewReceiverText(): String = parameterInfo.getInheritedName(containingCallable)

    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }
}

class KotlinImplicitThisUsage(
    callElement: KtElement,
    val targetDescriptor: DeclarationDescriptor
) : KotlinImplicitReceiverUsage(callElement) {
    override fun getNewReceiverText() = explicateReceiverOf(targetDescriptor)

    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true, removeThis = true))
    }
}

fun explicateReceiverOf(descriptor: DeclarationDescriptor): String {
    val name = descriptor.name
    return when {
        name.isSpecial -> "this"
        DescriptorUtils.isCompanionObject(descriptor) -> IdeDescriptorRenderers.SOURCE_CODE
            .renderClassifierName(descriptor as ClassifierDescriptor)
        else -> "this@${name.asString()}"
    }
}