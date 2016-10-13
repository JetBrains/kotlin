package org.jetbrains.uast

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

/**
 * A class wrapper to be used in [UastVisitor].
 */
interface UClass : UDeclaration, PsiClass {
    override val psi: PsiClass

    /**
     * Returns a [UClass] wrapper of the superclass of this class, or null if this class is [java.lang.Object].
     */
    val uastSuperClass: UClass?
        get() {
            val superClass = superClass ?: return null
            return getUastContext().convertWithParent(superClass)
        }

    /**
     * Returns [UDeclaration] wrappers for the class declarations.
     */
    val uastDeclarations: List<UDeclaration>
    
    val uastFields: List<UVariable>
    val uastInitializers: List<UClassInitializer>
    val uastMethods: List<UMethod>
    val uastNestedClasses: List<UClass>

    override fun asLogString() = "UClass (name = $name)"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        uastAnnotations.acceptList(visitor)
        uastDeclarations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }
}

interface UAnonymousClass : UClass, PsiAnonymousClass {
    override val psi: PsiAnonymousClass
}