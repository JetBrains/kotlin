/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiElement
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class SafeDeleteValueArgumentListUsageInfo(
    parameter: PsiElement,
    vararg valueArguments: KtValueArgument
) : SafeDeleteReferenceSimpleDeleteUsageInfo(valueArguments.first(), parameter, true) {
    private val valueArgumentPointers = valueArguments.map { it.createSmartPointer() }

    override fun deleteElement() {
        for (valueArgumentPointer in valueArgumentPointers) {
            val valueArgument = valueArgumentPointer.element ?: return
            val parent = valueArgument.parent
            if (parent is KtValueArgumentList) {
                parent.removeArgument(valueArgument)
            } else {
                valueArgument.delete()
            }
        }
    }
}
