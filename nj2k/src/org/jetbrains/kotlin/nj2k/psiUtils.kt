/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.codeInsight.generation.GenerateEqualsHelper.getEqualsSignature
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.j2k.isNullLiteral

//copied from old j2k
fun canKeepEqEq(left: PsiExpression, right: PsiExpression?): Boolean {
    if (left.isNullLiteral() || (right?.isNullLiteral() == true)) return true
    val type = left.type
    when (type) {
        is PsiPrimitiveType, is PsiArrayType -> return true

        is PsiClassType -> {
            if (right?.type is PsiPrimitiveType) return true

            val psiClass = type.resolve() ?: return false
            if (!psiClass.hasModifierProperty(PsiModifier.FINAL)) return false
            if (psiClass.isEnum) return true

            val equalsSignature = getEqualsSignature(left.project, GlobalSearchScope.allScope(left.project))
            val equalsMethod = MethodSignatureUtil.findMethodBySignature(psiClass, equalsSignature, true)
            if (equalsMethod != null && equalsMethod.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT) return false

            return true
        }

        else -> return false
    }

}