/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun PsiElement.getLabel(): JKElementInfoLabel? =
    prevSibling
        ?.safeAs<PsiComment>()
        ?.text
        ?.asLabel()
        ?: parent
            ?.safeAs<KtTypeProjection>()
            ?.getLabel()


fun PsiElement.elementInfo(converterContext: NewJ2kConverterContext): List<JKElementInfo>? =
    getLabel()?.let { label ->
        converterContext.elementsInfoStorage.getInfoForLabel(label)
    }


private fun KtTypeReference.hasUnknownLabel(context: NewJ2kConverterContext, unknownLabel: JKElementInfo) =
    getLabel()?.let { label ->
        unknownLabel in context.elementsInfoStorage.getInfoForLabel(label).orEmpty()
    } ?: false

fun KtTypeElement.hasUnknownLabel(context: NewJ2kConverterContext, unknownLabel: JKElementInfo) =
    parent
        ?.safeAs<KtTypeReference>()
        ?.hasUnknownLabel(context, unknownLabel) == true
