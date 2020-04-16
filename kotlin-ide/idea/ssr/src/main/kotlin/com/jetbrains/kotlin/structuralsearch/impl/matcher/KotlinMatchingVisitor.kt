package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.util.containers.toArray
import org.jetbrains.kotlin.psi.*
import kotlin.reflect.typeOf


class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    /**
     * Casts [myMatchingVisitor].element to [T], sets its result to false else.
     */
    private inline fun <reified T> getOther(): T? = when (myMatchingVisitor.element) {
        is T -> myMatchingVisitor.element as T
        else -> {
            myMatchingVisitor.result = false
            null
        }
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        val other = getOther<KtArrayAccessExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.arrayExpression, other.arrayExpression)
                && myMatchingVisitor.matchSons(expression.indicesNode, other.indicesNode)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        val other = getOther<KtBinaryExpression>() ?: return
        // Same operation
        if (myMatchingVisitor.setResult(expression.operationToken == other.operationToken)) {
            // Same operands
            myMatchingVisitor.result = myMatchingVisitor.match(expression.left, other.left)
                    && myMatchingVisitor.match(expression.right, other.right)
        }
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        myMatchingVisitor.result = myMatchingVisitor.element.text == expression.text
    }

    private inline fun <reified T : KtSimpleNameExpression> matchSimpleNameReferencedName(expr: KtReferenceExpression, expr2: T) {
        myMatchingVisitor.result = if (expr is T) expr.getReferencedName() == expr2.getReferencedName() else false
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val other = getOther<KtSimpleNameExpression>() ?: return
        when (expression) {
            is KtNameReferenceExpression -> matchSimpleNameReferencedName(other, expression)
            is KtLabelReferenceExpression -> matchSimpleNameReferencedName(other, expression)
            is KtOperationReferenceExpression -> matchSimpleNameReferencedName(other, expression)
            is KtEnumEntrySuperclassReferenceExpression -> matchSimpleNameReferencedName(other, expression)
        }
    }



}