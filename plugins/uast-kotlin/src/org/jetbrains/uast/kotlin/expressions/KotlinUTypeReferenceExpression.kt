package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.expressions.UTypeReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked

open class KotlinUTypeReferenceExpression(
        override val type: PsiType,
        override val psi: PsiElement?,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), UTypeReferenceExpression, PsiElementBacked, KotlinUElementWithType