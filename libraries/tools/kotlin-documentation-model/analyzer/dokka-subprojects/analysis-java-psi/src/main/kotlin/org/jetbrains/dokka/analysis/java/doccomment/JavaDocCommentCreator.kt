/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement

internal class JavaDocCommentCreator : DocCommentCreator {
    override fun create(element: PsiNamedElement): DocComment? {
        val psiDocComment = (element as? PsiDocCommentOwner)?.docComment ?: return null
        return JavaDocComment(psiDocComment)
    }
}
