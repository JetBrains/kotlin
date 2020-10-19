package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiWalkingState
import org.jetbrains.kotlin.psi.KtReferenceExpression

abstract class KotlinRecursiveElementWalkingVisitor : KtVisitor(), PsiRecursiveVisitor {
    private val myWalkingState: PsiWalkingState = object : PsiWalkingState(this) {
        override fun elementFinished(element: PsiElement) {}
    }

    override fun visitElement(element: PsiElement) {
        myWalkingState.elementStarted(element)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitExpression(expression)
        myWalkingState.startedWalking()
    }
}