package org.jetbrains.uast

import com.intellij.psi.PsiElement

interface UResolvable {
    /**
     * Resolve the reference.
     * Note that the reference is *always* resolved to an unwrapped [PsiElement], never to a [UElement].
     * 
     * @return the resolved element, or null if the reference couldn't be resolved.
     */
    fun resolve(): PsiElement?
}