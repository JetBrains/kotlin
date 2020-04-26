package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiWalkingState
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class KotlinRecursiveElementWalkingVisitor : KtVisitorVoid(), PsiRecursiveVisitor {

    private val myWalkingState: PsiWalkingState = object : PsiWalkingState(this) {
        override fun elementFinished(element: PsiElement) {
            this@KotlinRecursiveElementWalkingVisitor.elementFinished(element)
        }
    }

    override fun visitElement(element: PsiElement) {
        myWalkingState.elementStarted(element)
    }

    protected fun elementFinished(element: PsiElement) { }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitExpression(expression)
        myWalkingState.startedWalking()
    }

    fun stopWalking() {
        myWalkingState.stopWalking()
    }

}