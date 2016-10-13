package org.jetbrains.uast

import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

/**
 * A method visitor to be used in [UastVisitor].
 */
interface UMethod : UDeclaration, PsiMethod {
    override val psi: PsiMethod

    /**
     * Returns the body expression (which can be also a [UBlockExpression]).
     */
    val uastBody: UExpression?

    /**
     * Returns the method parameters.
     */
    val uastParameters: List<UParameter>

    @Deprecated("Use uastBody instead.", ReplaceWith("uastBody"))
    override fun getBody() = psi.body

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitMethod(this)) return
        uastAnnotations.acceptList(visitor)
        uastParameters.acceptList(visitor)
        uastBody?.accept(visitor)
        visitor.afterVisitMethod(this)
    }

    override fun asLogString() = "UMethod (name = $name)"
}

interface UAnnotationMethod : UMethod, PsiAnnotationMethod {
    override val psi: PsiAnnotationMethod

    /**
     * Returns the default value of this annotation method.
     */
    val uastDefaultValue: UExpression?

    override fun getDefaultValue() = psi.defaultValue

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitMethod(this)) return
        uastAnnotations.acceptList(visitor)
        uastParameters.acceptList(visitor)
        uastBody?.accept(visitor)
        uastDefaultValue?.accept(visitor)
        visitor.afterVisitMethod(this)
    }

    override fun asLogString() = "UAnnotationMethod (name = $name)"
}