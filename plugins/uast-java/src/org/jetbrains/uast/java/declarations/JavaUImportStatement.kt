package org.jetbrains.uast.java

import com.intellij.psi.PsiImportStatementBase
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement

class JavaUImportStatement(
        override val psi: PsiImportStatementBase, 
        override val containingElement: UElement?
) : UImportStatement {
    override val isOnDemand: Boolean
        get() = psi.isOnDemand
    override val importReference by lz { psi.importReference?.let { JavaDumbUElement(it, this) } }
    override fun resolve() = psi.resolve()
}