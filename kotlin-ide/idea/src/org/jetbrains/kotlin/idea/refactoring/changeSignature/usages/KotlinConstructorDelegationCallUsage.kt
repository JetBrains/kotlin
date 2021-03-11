/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class KotlinConstructorDelegationCallUsage(
    call: KtConstructorDelegationCall,
    changeInfo: KotlinChangeInfo
) : KotlinUsageInfo<KtConstructorDelegationCall>(call) {
    val delegate = KotlinFunctionCallUsage(call, changeInfo.methodDescriptor.originalPrimaryCallable)

    override fun processUsage(
        changeInfo: KotlinChangeInfo,
        element: KtConstructorDelegationCall,
        allUsages: Array<out UsageInfo>
    ): Boolean {
        val isThisCall = element.isCallToThis

        var elementToWorkWith = element
        if (changeInfo.getNewParametersCount() > 0 && element.isImplicit) {
            val constructor = element.parent as KtSecondaryConstructor
            elementToWorkWith = constructor.replaceImplicitDelegationCallWithExplicit(isThisCall)
        }

        val result = delegate.processUsage(changeInfo, elementToWorkWith, allUsages)

        if (changeInfo.getNewParametersCount() == 0 && !isThisCall && !elementToWorkWith.isImplicit) {
            (elementToWorkWith.parent as? KtSecondaryConstructor)?.colon?.delete()
            elementToWorkWith.replace(KtPsiFactory(element).creareDelegatedSuperTypeEntry(""))
        }

        return result
    }
}