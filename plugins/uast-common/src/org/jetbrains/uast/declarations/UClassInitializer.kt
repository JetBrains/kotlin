package org.jetbrains.uast

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

/**
 * A class initializer wrapper to be used in [UastVisitor].
 */
interface UClassInitializer : UDeclaration, PsiClassInitializer {
    override val psi: PsiClassInitializer

    /**
     * Returns the body of this class initializer.
     */
    val uastBody: UExpression

    @Deprecated("Use uastBody instead.", ReplaceWith("uastBody"))
    override fun getBody() = psi.body

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitInitializer(this)) return
        uastAnnotations.acceptList(visitor)
        uastBody.accept(visitor)
        visitor.afterVisitInitializer(this)
    }

    override fun asLogString() = "UMethod (name = ${psi.name}"
}