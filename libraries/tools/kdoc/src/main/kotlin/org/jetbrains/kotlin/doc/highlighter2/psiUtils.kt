package org.jetbrains.kotlin.doc.highlighter2

import com.intellij.psi.PsiElement

private fun PsiElement.getTextChildRelativeOffset() =
    getTextRange()!!.getStartOffset() - getParent()!!.getTextRange()!!.getStartOffset()

private fun PsiElement.getAllChildren(): List<PsiElement> {
    val r = arrayListOf<PsiElement>()
    var child = getFirstChild()
    while (child != null) {
        r.add(child!!)
        child = child!!.getNextSibling()
    }
    return r
}

private fun splitPsiImpl(psi: PsiElement, listBuilder: MutableList<Pair<String, PsiElement>>) {
    var lastPos = 0
    for (child in psi.getAllChildren()) {
        if (child.getTextChildRelativeOffset() > lastPos) {
            val text = psi.getText()!!.substring(lastPos, child.getTextChildRelativeOffset())
            listBuilder.add(Pair(text, psi))
        }
        splitPsiImpl(child, listBuilder)
        lastPos = child.getTextChildRelativeOffset() + child.getTextRange()!!.getLength()
    }
    if (lastPos < psi.getTextRange()!!.getLength()) {
        val text = psi.getText()!!.substring(lastPos)
        listBuilder.add(Pair(text, psi))
    }
}

fun splitPsi(psi: PsiElement): List<Pair<String, PsiElement>> {
    val listBuilder = arrayListOf<Pair<String, PsiElement>>()
    splitPsiImpl(psi, listBuilder)
    return listBuilder
}


