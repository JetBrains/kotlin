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
import org.jetbrains.uast.expressions.UTypeReferenceExpression

interface UastVisitor {
    fun visitElement(node: UElement): Boolean
    
    fun visitFile(node: UFile): Boolean = visitElement(node)
    fun visitImportStatement(node: UImportStatement): Boolean = visitElement(node)
    fun visitClass(node: UClass): Boolean = visitElement(node)
    fun visitInitializer(node: UClassInitializer): Boolean = visitElement(node)
    fun visitMethod(node: UMethod): Boolean = visitElement(node)
    fun visitVariable(node: UVariable): Boolean = visitElement(node)
    fun visitAnnotation(node: UAnnotation): Boolean = visitElement(node)

    // Expressions
    fun visitLabeledExpression(node: ULabeledExpression) = visitElement(node)
    fun visitDeclarationsExpression(node: UVariableDeclarationsExpression) = visitElement(node)
    fun visitBlockExpression(node: UBlockExpression) = visitElement(node)
    fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) = visitElement(node)
    fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) = visitElement(node)
    fun visitTypeReferenceExpression(node: UTypeReferenceExpression) = visitElement(node)
    fun visitCallExpression(node: UCallExpression) = visitElement(node)
    fun visitBinaryExpression(node: UBinaryExpression) = visitElement(node)
    fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) = visitElement(node)
    fun visitParenthesizedExpression(node: UParenthesizedExpression) = visitElement(node)
    fun visitUnaryExpression(node: UUnaryExpression) = visitElement(node)
    fun visitPrefixExpression(node: UPrefixExpression) = visitElement(node)
    fun visitPostfixExpression(node: UPostfixExpression) = visitElement(node)
    fun visitExpressionList(node: UExpressionList) = visitElement(node)
    fun visitIfExpression(node: UIfExpression) = visitElement(node)
    fun visitSwitchExpression(node: USwitchExpression) = visitElement(node)
    fun visitSwitchClauseExpression(node: USwitchClauseExpression) = visitElement(node)
    fun visitWhileExpression(node: UWhileExpression) = visitElement(node)
    fun visitDoWhileExpression(node: UDoWhileExpression) = visitElement(node)
    fun visitForExpression(node: UForExpression) = visitElement(node)
    fun visitForEachExpression(node: UForEachExpression) = visitElement(node)
    fun visitTryExpression(node: UTryExpression) = visitElement(node)
    fun visitCatchClause(node: UCatchClause) = visitElement(node)
    fun visitLiteralExpression(node: ULiteralExpression) = visitElement(node)
    fun visitThisExpression(node: UThisExpression) = visitElement(node)
    fun visitSuperExpression(node: USuperExpression) = visitElement(node)
    fun visitReturnExpression(node: UReturnExpression) = visitElement(node)
    fun visitBreakExpression(node: UBreakExpression) = visitElement(node)
    fun visitContinueExpression(node: UContinueExpression) = visitElement(node)
    fun visitThrowExpression(node: UThrowExpression) = visitElement(node)
    fun visitArrayAccessExpression(node: UArrayAccessExpression) = visitElement(node)
    fun visitCallableReferenceExpression(node: UCallableReferenceExpression) = visitElement(node)
    fun visitClassLiteralExpression(node: UClassLiteralExpression) = visitElement(node)
    fun visitLambdaExpression(node: ULambdaExpression) = visitElement(node)
    fun visitObjectLiteralExpression(node: UObjectLiteralExpression) = visitElement(node)
    
    // After

    fun afterVisitElement(node: UElement) {}

    fun afterVisitFile(node: UFile) { afterVisitElement(node) }
    fun afterVisitImportStatement(node: UImportStatement) { afterVisitElement(node) }
    fun afterVisitClass(node: UClass) { afterVisitElement(node) }
    fun afterVisitInitializer(node: UClassInitializer) { afterVisitElement(node) }
    fun afterVisitMethod(node: UMethod) { afterVisitElement(node) }
    fun afterVisitVariable(node: UVariable) { afterVisitElement(node) }
    fun afterVisitAnnotation(node: UAnnotation) { afterVisitElement(node) }

    // Expressions
    fun afterVisitLabeledExpression(node: ULabeledExpression) { afterVisitElement(node) }
    fun afterVisitDeclarationsExpression(node: UVariableDeclarationsExpression) { afterVisitElement(node) }
    fun afterVisitBlockExpression(node: UBlockExpression) { afterVisitElement(node) }
    fun afterVisitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) { afterVisitElement(node) }
    fun afterVisitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) { afterVisitElement(node) }
    fun afterVisitTypeReferenceExpression(node: UTypeReferenceExpression) { afterVisitElement(node) }
    fun afterVisitCallExpression(node: UCallExpression) { afterVisitElement(node) }
    fun afterVisitBinaryExpression(node: UBinaryExpression) { afterVisitElement(node) }
    fun afterVisitBinaryExpressionWithType(node: UBinaryExpressionWithType) { afterVisitElement(node) }
    fun afterVisitParenthesizedExpression(node: UParenthesizedExpression) { afterVisitElement(node) }
    fun afterVisitUnaryExpression(node: UUnaryExpression) { afterVisitElement(node) }
    fun afterVisitPrefixExpression(node: UPrefixExpression) { afterVisitElement(node) }
    fun afterVisitPostfixExpression(node: UPostfixExpression) { afterVisitElement(node) }
    fun afterVisitExpressionList(node: UExpressionList) { afterVisitElement(node) }
    fun afterVisitIfExpression(node: UIfExpression) { afterVisitElement(node) }
    fun afterVisitSwitchExpression(node: USwitchExpression) { afterVisitElement(node) }
    fun afterVisitSwitchClauseExpression(node: USwitchClauseExpression) { afterVisitElement(node) }
    fun afterVisitWhileExpression(node: UWhileExpression) { afterVisitElement(node) }
    fun afterVisitDoWhileExpression(node: UDoWhileExpression) { afterVisitElement(node) }
    fun afterVisitForExpression(node: UForExpression) { afterVisitElement(node) }
    fun afterVisitForEachExpression(node: UForEachExpression) { afterVisitElement(node) }
    fun afterVisitTryExpression(node: UTryExpression) { afterVisitElement(node) }
    fun afterVisitCatchClause(node: UCatchClause) { afterVisitElement(node) }
    fun afterVisitLiteralExpression(node: ULiteralExpression) { afterVisitElement(node) }
    fun afterVisitThisExpression(node: UThisExpression) { afterVisitElement(node) }
    fun afterVisitSuperExpression(node: USuperExpression) { afterVisitElement(node) }
    fun afterVisitReturnExpression(node: UReturnExpression) { afterVisitElement(node) }
    fun afterVisitBreakExpression(node: UBreakExpression) { afterVisitElement(node) }
    fun afterVisitContinueExpression(node: UContinueExpression) { afterVisitElement(node) }
    fun afterVisitThrowExpression(node: UThrowExpression) { afterVisitElement(node) }
    fun afterVisitArrayAccessExpression(node: UArrayAccessExpression) { afterVisitElement(node) }
    fun afterVisitCallableReferenceExpression(node: UCallableReferenceExpression) { afterVisitElement(node) }
    fun afterVisitClassLiteralExpression(node: UClassLiteralExpression) { afterVisitElement(node) }
    fun afterVisitLambdaExpression(node: ULambdaExpression) { afterVisitElement(node) }
    fun afterVisitObjectLiteralExpression(node: UObjectLiteralExpression) { afterVisitElement(node) }
}

abstract class AbstractUastVisitor : UastVisitor {
    override fun visitElement(node: UElement): Boolean = false
    
}

object EmptyUastVisitor : AbstractUastVisitor()