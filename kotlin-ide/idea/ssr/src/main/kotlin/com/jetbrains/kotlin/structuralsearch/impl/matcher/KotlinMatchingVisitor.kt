package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import org.jetbrains.kotlin.psi.*


class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    /**
     * Casts the current code element to [T], sets [myMatchingVisitor].result to false else.
     */
    private inline fun <reified T> getTreeElement(): T? = when (myMatchingVisitor.element) {
        is T -> myMatchingVisitor.element as T
        else -> {
            myMatchingVisitor.result = false
            null
        }
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        val other = getTreeElement<KtArrayAccessExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.arrayExpression, other.arrayExpression)
                && myMatchingVisitor.matchSons(expression.indicesNode, other.indicesNode)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        val other = getTreeElement<KtBinaryExpression>() ?: return
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

    private inline fun <reified T : KtSimpleNameExpression> matchSimpleNameReferencedName(patternElement: T, treeElement: KtReferenceExpression) {
        myMatchingVisitor.result =
            treeElement is T && patternElement.getReferencedName() == treeElement.getReferencedName()
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val other = getTreeElement<KtSimpleNameExpression>() ?: return
        when (expression) {
            is KtNameReferenceExpression -> matchSimpleNameReferencedName(expression, other)
            is KtLabelReferenceExpression -> matchSimpleNameReferencedName(expression, other)
            is KtOperationReferenceExpression -> matchSimpleNameReferencedName(expression, other)
            is KtEnumEntrySuperclassReferenceExpression -> matchSimpleNameReferencedName(expression, other)
        }
    }

    private inline fun <reified T : KtExpressionWithLabel> matchExpressionWithLabel(patternElement: T, treeElement: KtExpressionWithLabel) {
        myMatchingVisitor.result =
            treeElement is T && myMatchingVisitor.match(patternElement.getTargetLabel(), treeElement.getTargetLabel())
    }

    override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
        val other = getTreeElement<KtExpressionWithLabel>() ?: return
        when (expression) {
            is KtBreakExpression -> matchExpressionWithLabel(expression, other)
            is KtContinueExpression -> matchExpressionWithLabel(expression, other)
            is KtSuperExpression -> matchExpressionWithLabel(expression, other)
            is KtThisExpression -> matchExpressionWithLabel(expression, other)
            is KtReturnExpression -> {
                myMatchingVisitor.result = other is KtReturnExpression
                        && myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
                        && myMatchingVisitor.match(expression.returnedExpression, other.returnedExpression)
            }
        }
    }

}