/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall

abstract class JavaMethodKotlinUsageWithDelegate<T : PsiElement>(
    val psiElement: T,
    var javaMethodChangeInfo: KotlinChangeInfo
) : UsageInfo(psiElement) {
    abstract val delegateUsage: KotlinUsageInfo<T>

    fun processUsage(allUsages: Array<UsageInfo>): Boolean = delegateUsage.processUsage(javaMethodChangeInfo, psiElement, allUsages)
}

class JavaMethodKotlinCallUsage(
    callElement: KtCallElement,
    javaMethodChangeInfo: KotlinChangeInfo,
    propagationCall: Boolean
) : JavaMethodKotlinUsageWithDelegate<KtCallElement>(callElement, javaMethodChangeInfo) {
    @Suppress("UNCHECKED_CAST")
    override val delegateUsage = when {
        propagationCall -> KotlinCallerCallUsage(psiElement)
        psiElement is KtConstructorDelegationCall -> KotlinConstructorDelegationCallUsage(psiElement, javaMethodChangeInfo)
        else -> KotlinFunctionCallUsage(psiElement, javaMethodChangeInfo.methodDescriptor.originalPrimaryCallable)
    } as KotlinUsageInfo<KtCallElement>
}
