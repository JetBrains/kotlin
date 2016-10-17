package org.jetbrains.uast

import com.intellij.psi.PsiClass
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

/**
 * An annotation wrapper to be used in [UastVisitor].
 */
interface UAnnotation : UElement, PsiElementBacked, UResolvable {
    /**
     * Returns the annotation qualified name.
     */
    val qualifiedName: String?

    /**
     * Returns the annotation class, or null if the class reference was not resolved.
     */
    override fun resolve(): PsiClass?

    /**
     * Returns the annotation values.
     */
    val attributeValues: List<UNamedExpression>

    fun findAttributeValue(name: String?): UNamedExpression?

    fun findDeclaredAttributeValue(name: String?): UNamedExpression?

    override fun asLogString() = "UAnnotation"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitAnnotation(this)) return
        attributeValues.acceptList(visitor)
        visitor.afterVisitAnnotation(this)
    }
}