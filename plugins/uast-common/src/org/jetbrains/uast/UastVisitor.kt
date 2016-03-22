/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast.visitor

import org.jetbrains.uast.*

abstract class UastVisitor {
    open fun visitFile(node: UFile): Boolean = false

    // Declarations
    open fun visitClass(node: UClass): Boolean = false
    open fun visitFunction(node: UFunction): Boolean = false
    open fun visitVariable(node: UVariable): Boolean = false
    open fun visitImportStatement(node: UImportStatement): Boolean = false
    open fun visitAnnotation(node: UAnnotation): Boolean = false

    // Expressions
    open fun visitLabeledExpression(node: ULabeledExpression): Boolean = false
    open fun visitDeclarationsExpression(node: UDeclarationsExpression): Boolean = false
    open fun visitBlockExpression(node: UBlockExpression): Boolean = false
    open fun visitQualifiedExpression(node: UQualifiedExpression): Boolean = false
    open fun visitSimpleReferenceExpression(node: USimpleReferenceExpression): Boolean = false
    open fun visitCallExpression(node: UCallExpression): Boolean = false
    open fun visitAssignmentExpression(node: UAssignmentExpression): Boolean = false
    open fun visitBinaryExpression(node: UBinaryExpression): Boolean = false
    open fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType): Boolean = false
    open fun visitParenthesizedExpression(node: UParenthesizedExpression): Boolean = false
    open fun visitPrefixExpression(node: UPrefixExpression): Boolean = false
    open fun visitPostfixExpression(node: UPostfixExpression): Boolean = false
    open fun visitSpecialExpressionList(node: USpecialExpressionList): Boolean = false
    open fun visitExpressionList(node: UExpressionList): Boolean = false
    open fun visitIfExpression(node: UIfExpression): Boolean = false
    open fun visitSwitchExpression(node: USwitchExpression): Boolean = false
    open fun visitSwitchClauseExpression(node: USwitchClauseExpression): Boolean = false
    open fun visitWhileExpression(node: UWhileExpression): Boolean = false
    open fun visitDoWhileExpression(node: UDoWhileExpression): Boolean = false
    open fun visitForExpression(node: UForExpression): Boolean = false
    open fun visitForEachExpression(node: UForEachExpression): Boolean = false
    open fun visitTryExpression(node: UTryExpression): Boolean = false
    open fun visitLiteralExpression(node: ULiteralExpression): Boolean = false
    open fun visitThisExpression(node: UThisExpression): Boolean = false
    open fun visitSuperExpression(node: USuperExpression): Boolean = false

    open fun beforeVisit(node: UElement) {}
    open fun afterVisit(node: UElement) {}

    fun handle(node: UElement): Boolean {
        beforeVisit(node)
        val result = when (node) {
            is UFile -> visitFile(node)

            is UClass -> visitClass(node)
            is UFunction -> visitFunction(node)
            is UVariable -> visitVariable(node)
            is UImportStatement -> visitImportStatement(node)
            is UAnnotation -> visitAnnotation(node)

            is ULabeledExpression -> visitLabeledExpression(node)
            is UDeclarationsExpression -> visitDeclarationsExpression(node)
            is UBlockExpression -> visitBlockExpression(node)
            is UQualifiedExpression -> visitQualifiedExpression(node)
            is USimpleReferenceExpression -> visitSimpleReferenceExpression(node)
            is UCallExpression -> visitCallExpression(node)
            is UAssignmentExpression -> visitAssignmentExpression(node)
            is UBinaryExpression -> visitBinaryExpression(node)
            is UBinaryExpressionWithType -> visitBinaryExpressionWithType(node)
            is UParenthesizedExpression -> visitParenthesizedExpression(node)
            is UPrefixExpression -> visitPrefixExpression(node)
            is UPostfixExpression -> visitPostfixExpression(node)
            is USpecialExpressionList -> visitSpecialExpressionList(node)
            is UExpressionList -> visitExpressionList(node)
            is UIfExpression -> visitIfExpression(node)
            is USwitchExpression -> visitSwitchExpression(node)
            is USwitchClauseExpression -> visitSwitchClauseExpression(node)
            is UWhileExpression -> visitWhileExpression(node)
            is UDoWhileExpression -> visitDoWhileExpression(node)
            is UForExpression -> visitForExpression(node)
            is UForEachExpression -> visitForEachExpression(node)
            is UTryExpression -> visitTryExpression(node)
            is ULiteralExpression -> visitLiteralExpression(node)
            is UThisExpression -> visitThisExpression(node)
            is USuperExpression -> visitSuperExpression(node)
            else -> true
        }
        afterVisit(node)
        return result
    }
    
    open fun process(element: UElement) {
        if (!handle(element)) {
            processChildren(element)
        }
    }

    fun processChildren(element: UElement) {
        element.traverse(UastHandler { handle(it) })
    }
}

object EmptyUastVisitor : UastVisitor() {
    override fun process(element: UElement) {}
}