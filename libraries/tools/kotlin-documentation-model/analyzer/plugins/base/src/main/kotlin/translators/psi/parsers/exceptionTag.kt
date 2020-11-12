package org.jetbrains.dokka.base.translators.psi.parsers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.util.PsiTreeUtil

internal fun PsiElement.referenceElementOrSelf(): PsiElement? =
    if (node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER) {
        PsiTreeUtil.findChildOfType(this, PsiJavaCodeReferenceElement::class.java)
    } else this

internal fun PsiElement.resolveToGetDri(): PsiElement? =
    reference?.resolve()