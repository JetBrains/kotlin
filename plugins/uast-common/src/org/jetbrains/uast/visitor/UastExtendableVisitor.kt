/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.uast.visitor.UastVisitor

class UastExtendableVisitor(
        private val original: UastVisitor,
        private val context: UastContext,
        private val extensions: List<UastVisitorExtension>
) : UastVisitor {
    private fun handleExtensions(node: UElement) {
        if (node !is SynthesizedUElement) {
            for (extension in extensions) {
                extension.invoke(node, this, context)
            }
        }
    }

    override fun visitElement(node: UElement): Boolean {
        handleExtensions(node)
        return original.visitElement(node)
    }

    override fun visitFile(node: UFile): Boolean {
        handleExtensions(node)
        return original.visitFile(node)
    }

    override fun visitImportStatement(node: UImportStatement): Boolean {
        handleExtensions(node)
        return original.visitImportStatement(node)
    }

    override fun visitAnnotation(node: UAnnotation): Boolean {
        handleExtensions(node)
        return original.visitAnnotation(node)
    }

    override fun visitCatchClause(node: UCatchClause): Boolean {
        handleExtensions(node)
        return original.visitCatchClause(node)
    }

    override fun visitType(node: UType): Boolean {
        handleExtensions(node)
        return original.visitType(node)
    }

    override fun visitClass(node: UClass): Boolean {
        handleExtensions(node)
        return original.visitClass(node)
    }

    override fun visitFunction(node: UFunction): Boolean {
        handleExtensions(node)
        return original.visitFunction(node)
    }

    override fun visitVariable(node: UVariable): Boolean {
        handleExtensions(node)
        return original.visitVariable(node)
    }

    override fun visitLabeledExpression(node: ULabeledExpression): Boolean {
        handleExtensions(node)
        return original.visitLabeledExpression(node)
    }

    override fun visitDeclarationsExpression(node: UDeclarationsExpression): Boolean {
        handleExtensions(node)
        return original.visitDeclarationsExpression(node)
    }

    override fun visitBlockExpression(node: UBlockExpression): Boolean {
        handleExtensions(node)
        return original.visitBlockExpression(node)
    }

    override fun visitQualifiedExpression(node: UQualifiedExpression): Boolean {
        handleExtensions(node)
        return original.visitQualifiedExpression(node)
    }

    override fun visitSimpleReferenceExpression(node: USimpleReferenceExpression): Boolean {
        handleExtensions(node)
        return original.visitSimpleReferenceExpression(node)
    }

    override fun visitCallExpression(node: UCallExpression): Boolean {
        handleExtensions(node)
        return original.visitCallExpression(node)
    }

    override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
        handleExtensions(node)
        return original.visitBinaryExpression(node)
    }

    override fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType): Boolean {
        handleExtensions(node)
        return original.visitBinaryExpressionWithType(node)
    }

    override fun visitParenthesizedExpression(node: UParenthesizedExpression): Boolean {
        handleExtensions(node)
        return original.visitParenthesizedExpression(node)
    }

    override fun visitUnaryExpression(node: UUnaryExpression): Boolean {
        handleExtensions(node)
        return original.visitUnaryExpression(node)
    }

    override fun visitPrefixExpression(node: UPrefixExpression): Boolean {
        handleExtensions(node)
        return original.visitPrefixExpression(node)
    }

    override fun visitPostfixExpression(node: UPostfixExpression): Boolean {
        handleExtensions(node)
        return original.visitPostfixExpression(node)
    }

    override fun visitSpecialExpressionList(node: USpecialExpressionList): Boolean {
        handleExtensions(node)
        return original.visitSpecialExpressionList(node)
    }

    override fun visitIfExpression(node: UIfExpression): Boolean {
        handleExtensions(node)
        return original.visitIfExpression(node)
    }

    override fun visitSwitchExpression(node: USwitchExpression): Boolean {
        handleExtensions(node)
        return original.visitSwitchExpression(node)
    }

    override fun visitSwitchClauseExpression(node: USwitchClauseExpression): Boolean {
        handleExtensions(node)
        return original.visitSwitchClauseExpression(node)
    }

    override fun visitWhileExpression(node: UWhileExpression): Boolean {
        handleExtensions(node)
        return original.visitWhileExpression(node)
    }

    override fun visitDoWhileExpression(node: UDoWhileExpression): Boolean {
        handleExtensions(node)
        return original.visitDoWhileExpression(node)
    }

    override fun visitForExpression(node: UForExpression): Boolean {
        handleExtensions(node)
        return original.visitForExpression(node)
    }

    override fun visitForEachExpression(node: UForEachExpression): Boolean {
        handleExtensions(node)
        return original.visitForEachExpression(node)
    }

    override fun visitTryExpression(node: UTryExpression): Boolean {
        handleExtensions(node)
        return original.visitTryExpression(node)
    }

    override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
        handleExtensions(node)
        return original.visitLiteralExpression(node)
    }

    override fun visitThisExpression(node: UThisExpression): Boolean {
        handleExtensions(node)
        return original.visitThisExpression(node)
    }

    override fun visitSuperExpression(node: USuperExpression): Boolean {
        handleExtensions(node)
        return original.visitSuperExpression(node)
    }

    override fun visitReturnExpression(node: UReturnExpression): Boolean {
        handleExtensions(node)
        return super.visitReturnExpression(node)
    }

    override fun visitBreakExpression(node: UBreakExpression): Boolean {
        handleExtensions(node)
        return original.visitBreakExpression(node)
    }

    override fun visitContinueExpression(node: UContinueExpression): Boolean {
        handleExtensions(node)
        return original.visitContinueExpression(node)
    }

    override fun visitThrowExpression(node: UThrowExpression): Boolean {
        handleExtensions(node)
        return original.visitThrowExpression(node)
    }

    override fun visitArrayAccessExpression(node: UArrayAccessExpression): Boolean {
        handleExtensions(node)
        return original.visitArrayAccessExpression(node)
    }

    override fun visitCallableReferenceExpression(node: UCallableReferenceExpression): Boolean {
        handleExtensions(node)
        return original.visitCallableReferenceExpression(node)
    }

    override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {
        handleExtensions(node)
        return original.visitClassLiteralExpression(node)
    }

    override fun visitLambdaExpression(node: ULambdaExpression): Boolean {
        handleExtensions(node)
        return original.visitLambdaExpression(node)
    }

    override fun visitObjectLiteralExpression(node: UObjectLiteralExpression): Boolean {
        handleExtensions(node)
        return original.visitObjectLiteralExpression(node)
    }
}