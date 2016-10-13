package org.jetbrains.uast

import com.intellij.psi.PsiAnnotation

class SimpleUAnnotation(
        psi: PsiAnnotation, 
        override val containingElement: UElement?
) : UAnnotation, PsiAnnotation by psi {
    override val psi: PsiAnnotation = unwrap(psi)

    private companion object {
        fun unwrap(psi: PsiAnnotation): PsiAnnotation {
            val unwrapped = if (psi is UAnnotation) psi.psi else psi
            assert(unwrapped !is UElement)
            return unwrapped
        }
    }
}