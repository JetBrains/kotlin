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
    open fun visitElement(node: UElement): Boolean = false

    open fun visitFile(node: UFile) = visitElement(node)
    open fun visitImportStatement(node: UImportStatement) = visitElement(node)
    open fun visitAnnotation(node: UAnnotation) = visitElement(node)
    open fun visitCatchClause(node: UCatchClause) = visitElement(node)
    open fun visitType(node: UType) = visitElement(node)

    // Declarations
    open fun visitClass(node: UClass) = visitElement(node)
    open fun visitFunction(node: UFunction) = visitElement(node)
    open fun visitVariable(node: UVariable) = visitElement(node)


    // Expressions
    open fun visitLabeledExpression(node: ULabeledExpression) = visitElement(node)
    open fun visitDeclarationsExpression(node: UDeclarationsExpression) = visitElement(node)
    open fun visitBlockExpression(node: UBlockExpression) = visitElement(node)
    open fun visitQualifiedExpression(node: UQualifiedExpression) = visitElement(node)
    open fun visitSimpleReferenceExpression(node: USimpleReferenceExpression) = visitElement(node)
    open fun visitCallExpression(node: UCallExpression) = visitElement(node)
    open fun visitBinaryExpression(node: UBinaryExpression) = visitElement(node)
    open fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) = visitElement(node)
    open fun visitParenthesizedExpression(node: UParenthesizedExpression) = visitElement(node)
    open fun visitUnaryExpression(node: UUnaryExpression) = visitElement(node)
    open fun visitPrefixExpression(node: UPrefixExpression) = visitElement(node)
    open fun visitPostfixExpression(node: UPostfixExpression) = visitElement(node)
    open fun visitSpecialExpressionList(node: USpecialExpressionList) = visitElement(node)
    open fun visitExpressionList(node: UExpressionList) = visitElement(node)
    open fun visitIfExpression(node: UIfExpression) = visitElement(node)
    open fun visitSwitchExpression(node: USwitchExpression) = visitElement(node)
    open fun visitSwitchClauseExpression(node: USwitchClauseExpression) = visitElement(node)
    open fun visitWhileExpression(node: UWhileExpression) = visitElement(node)
    open fun visitDoWhileExpression(node: UDoWhileExpression) = visitElement(node)
    open fun visitForExpression(node: UForExpression) = visitElement(node)
    open fun visitForEachExpression(node: UForEachExpression) = visitElement(node)
    open fun visitTryExpression(node: UTryExpression) = visitElement(node)
    open fun visitLiteralExpression(node: ULiteralExpression) = visitElement(node)
    open fun visitThisExpression(node: UThisExpression) = visitElement(node)
    open fun visitSuperExpression(node: USuperExpression) = visitElement(node)
    open fun visitArrayAccessExpression(node: UArrayAccessExpression) = visitElement(node)
    open fun visitCallableReferenceExpression(node: UCallableReferenceExpression) = visitElement(node)
    open fun visitClassLiteralExpression(node: UClassLiteralExpression) = visitElement(node)
    open fun visitLambdaExpression(node: ULambdaExpression) = visitElement(node)
    open fun visitObjectLiteralExpression(node: UObjectLiteralExpression) = visitElement(node)
}

object EmptyUastVisitor : UastVisitor()