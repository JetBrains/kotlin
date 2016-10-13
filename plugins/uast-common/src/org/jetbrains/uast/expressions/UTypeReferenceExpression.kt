package org.jetbrains.uast.expressions

import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.name
import org.jetbrains.uast.visitor.UastVisitor

interface UTypeReferenceExpression : UExpression {
    /**
     * Returns the resolved type for this reference.
     */
    val type: PsiType

    /**
     * Returns the qualified name of the class type, or null if the [type] is not a class type.
     */
    fun getQualifiedName() = PsiTypesUtil.getPsiClass(type)?.qualifiedName

    override fun accept(visitor: UastVisitor) {
        visitor.visitTypeReferenceExpression(this)
        visitor.afterVisitTypeReferenceExpression(this)
    }

    override fun asLogString() = "UTypeReferenceExpression (${type.name})"
    override fun asRenderString() = type.name
}