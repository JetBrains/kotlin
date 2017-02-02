package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked

open class KotlinUTypeReferenceExpression(
        override val type: PsiType,
        override val psi: PsiElement?,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), UTypeReferenceExpression, PsiElementBacked, KotlinUElementWithType


class LazyKotlinUTypeReferenceExpression(
        override val psi: PsiElement,
        override val containingElement: UElement?,
        private val typeSupplier: () -> PsiType
) : KotlinAbstractUExpression(), UTypeReferenceExpression, PsiElementBacked {
    override val type: PsiType by lz { typeSupplier() }
}