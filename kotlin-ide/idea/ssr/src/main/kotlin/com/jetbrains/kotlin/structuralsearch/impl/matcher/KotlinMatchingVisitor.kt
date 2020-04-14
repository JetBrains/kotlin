package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid


class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    override fun visitElement(element: PsiElement?) {
        myMatchingVisitor.result = myMatchingVisitor.matchText(element, myMatchingVisitor.element)
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        myMatchingVisitor.result = myMatchingVisitor.element.text == expression.text
        super.visitConstantExpression(expression)
    }

}