package org.jetbrains.uast

import com.intellij.psi.PsiElement
import org.jetbrains.uast.psi.PsiElementBacked

class UComment(override val psi: PsiElement, override val containingElement: UElement) : UElement, PsiElementBacked {
    val text: String
        get() = asSourceString()

    override fun asLogString() = "UComment"
    override fun asRenderString(): String = asSourceString()
    override fun asSourceString(): String = psi.text
}