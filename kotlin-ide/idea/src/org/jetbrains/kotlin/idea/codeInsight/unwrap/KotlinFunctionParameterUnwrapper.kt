/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.unwrap

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinFunctionParameterUnwrapper(key: String) : KotlinUnwrapRemoveBase(key) {

    override fun isApplicableTo(element: PsiElement): Boolean {
        if (element !is KtCallExpression) return false
        if (element.valueArguments.size != 1) return false
        val argument = element.parent as? KtValueArgument ?: return false
        if (argument.getStrictParentOfType<KtCallExpression>() == null) return false
        return true
    }

    override fun doUnwrap(element: PsiElement?, context: Context?) {
        val function = element as? KtCallExpression ?: return
        val argument = element.valueArguments.firstOrNull()?.getArgumentExpression() ?: return
        context?.extractFromExpression(argument, function)
        context?.delete(function)
    }

}
