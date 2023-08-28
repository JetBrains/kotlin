package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentCreator
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.findKDoc
import org.jetbrains.kotlin.psi.KtElement

internal class DescriptorKotlinDocCommentCreator : DocCommentCreator {
    override fun create(element: PsiNamedElement): DocComment? {
        val ktElement = element.navigationElement as? KtElement ?: return null
        val kdoc = ktElement.findKDoc() ?: return null

        return KotlinDocComment(kdoc.contentTag, ResolveDocContext(ktElement))
    }
}
