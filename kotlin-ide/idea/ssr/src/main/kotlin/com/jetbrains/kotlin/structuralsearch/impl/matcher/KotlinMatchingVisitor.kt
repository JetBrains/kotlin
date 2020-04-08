package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid


class KotlinMatchingVisitor(private val myWatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    override fun visitConstantExpression(expression: KtConstantExpression) {
        println("Visited constant ${expression.text}")

        visitExpression(expression)
    }

}