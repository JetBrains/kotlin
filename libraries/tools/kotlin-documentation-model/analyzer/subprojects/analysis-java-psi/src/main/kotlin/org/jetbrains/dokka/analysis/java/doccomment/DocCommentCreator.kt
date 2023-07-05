package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
interface DocCommentCreator {
    fun create(element: PsiNamedElement): DocComment?
}
