package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class KotlinRecursiveElementVisitor : KtVisitorVoid(), PsiRecursiveVisitor {
    private val myRefsExprsInVisit = Stack<KtReferenceExpression>()

    override fun visitElement(element: PsiElement) {
        if (myRefsExprsInVisit.isNotEmpty() && myRefsExprsInVisit.peek() == element) {
            myRefsExprsInVisit.pop()
            myRefsExprsInVisit.push(null)
        } else {
            element.acceptChildren(this)
        }
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        myRefsExprsInVisit.push(expression)
        try {
            visitExpression(expression)
        } finally {
            myRefsExprsInVisit.pop()
        }
    }
}