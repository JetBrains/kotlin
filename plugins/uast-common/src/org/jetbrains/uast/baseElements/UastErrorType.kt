package org.jetbrains.uast

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeVisitor

object UastErrorType : PsiType(emptyArray()) {
    override fun getInternalCanonicalText() = "<ErrorType>"
    override fun equalsToText(text: String) = false
    override fun getCanonicalText() = internalCanonicalText
    override fun getPresentableText() = canonicalText
    override fun isValid() = false
    override fun getResolveScope() = null
    override fun getSuperTypes() = emptyArray<PsiType>()

    override fun <A : Any?> accept(visitor: PsiTypeVisitor<A>) = visitor.visitType(this)
}