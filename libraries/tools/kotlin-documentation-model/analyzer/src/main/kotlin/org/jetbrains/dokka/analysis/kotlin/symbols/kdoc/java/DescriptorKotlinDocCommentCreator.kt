/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentCreator
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.findKDoc
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNonPublicApi

internal class DescriptorKotlinDocCommentCreator : DocCommentCreator {
    @OptIn(KtNonPublicApi::class, KaNonPublicApi::class)
    override fun create(element: PsiNamedElement): DocComment? {
        val ktElement = element.navigationElement as? KtElement ?: return null
        val kdoc = (ktElement as? KtDeclaration)?.findKDoc() ?: return null

        return KotlinDocComment(kdoc.primaryTag, ResolveDocContext(ktElement))
    }
}
