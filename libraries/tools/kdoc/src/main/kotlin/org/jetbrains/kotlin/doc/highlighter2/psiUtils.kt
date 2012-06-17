package org.jetbrains.kotlin.doc.highlighter2

import org.jetbrains.jet.internal.com.intellij.psi.PsiElement
import java.util.List

private fun PsiElement.getTextChildRelativeOffset() =
    getTextRange()!!.getStartOffset() - getParent()!!.getTextRange()!!.getStartOffset()

private fun PsiElement.getAllChildren(): List<PsiElement> {
    val r = listBuilder<PsiElement>()
    var child = getFirstChild()
    while (child != null) {
        r.add(child!!)
        child = child!!.getNextSibling()
    }
    return r.build()
}

private fun splitPsiImpl(psi: PsiElement, listBuilder: ImmutableArrayListBuilder<#(String, PsiElement)>) {
    var lastPos = 0
    for (child in psi.getAllChildren()) {
        if (child.getTextChildRelativeOffset() > lastPos) {
            val text = psi.getText()!!.substring(lastPos, child.getTextChildRelativeOffset())
            listBuilder.add(#(text, psi))
        }
        splitPsiImpl(child, listBuilder)
        lastPos = child.getTextChildRelativeOffset() + child.getTextRange()!!.getLength()
    }
    if (lastPos < psi.getTextRange()!!.getLength()) {
        val text = psi.getText()!!.substring(lastPos)
        listBuilder.add(#(text, psi))
    }
}

fun splitPsi(psi: PsiElement): List<#(String, PsiElement)> {
    val listBuilder = listBuilder<#(String, PsiElement)>()
    splitPsiImpl(psi, listBuilder)
    return listBuilder.build()
}


