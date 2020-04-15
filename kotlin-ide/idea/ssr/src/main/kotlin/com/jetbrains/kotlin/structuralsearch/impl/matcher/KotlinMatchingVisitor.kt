package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid


class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        val other = myMatchingVisitor.element
        if (myMatchingVisitor.setResult(other is KtBinaryExpression)) {
            val expr2 = other as KtBinaryExpression
            // Same operation
            if (myMatchingVisitor.setResult(expression.operationToken == expr2.operationToken)) {
                // Same operands
                myMatchingVisitor.result = myMatchingVisitor.match(expression.left, expr2.left)
                        && myMatchingVisitor.match(expression.right, expr2.right)
            }
        }
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        myMatchingVisitor.result = myMatchingVisitor.element.text == expression.text
    }

}