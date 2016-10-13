package org.jetbrains.uast

import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents an import statement.
 */
interface UImportStatement : UResolvable, UElement, PsiElementBacked {
    /**
     * Returns true if the statement is an import-on-demand (star-import) statement.
     */
    val isOnDemand: Boolean

    /**
     * Returns the reference to the imported element.
     */
    val importReference: UElement?
    
    override fun asLogString() = "UImportStatement (onDemand = $isOnDemand)"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitImportStatement(this)) return
        visitor.afterVisitImportStatement(this)
    }
}