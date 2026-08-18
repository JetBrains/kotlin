/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList

internal class JavaDocCommentCreator : DocCommentCreator {
    override fun create(element: PsiNamedElement): DocComment? {
        val psiDocComment = if(element is PsiParameter && element.parent is PsiParameterList)
            (element.parent.parent as? PsiDocCommentOwner)?.docComment ?: return null
        else
            (element as? PsiDocCommentOwner)?.docComment ?: return null
        return JavaDocComment(psiDocComment)
    }
}
