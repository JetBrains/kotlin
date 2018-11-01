/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.j2k.tree.JKBinaryExpression
import org.jetbrains.kotlin.j2k.tree.JKClassType
import org.jetbrains.kotlin.j2k.tree.JKJavaInstanceOfExpression
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.psi.KtClass


class InstanceOfConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaInstanceOfExpression) return recurse(element)
        val checkingType = element.type.type
        val type =
            if (checkingType is JKClassType && checkingType.parameters.isEmpty()) {
                val resolvedClass = checkingType.classReference?.target
                val parametersCount =
                    when (resolvedClass) {
                        is PsiClass -> resolvedClass.typeParameters.size
                        is KtClass -> resolvedClass.typeParameters.size
                        else -> 0
                    }
                val typeParameters = List(parametersCount) { JKStarProjectionTypeImpl() }
                JKClassTypeImpl(
                    checkingType.classReference as JKClassSymbol,
                    typeParameters,
                    checkingType.nullability
                )

            } else checkingType
        return recurse(JKKtIsExpressionImpl(element.expression.also { it.detach(it.parent!!) }, JKTypeElementImpl(type)))
    }
}