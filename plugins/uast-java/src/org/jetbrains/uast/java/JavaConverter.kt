/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package org.jetbrains.uast.java

import com.intellij.psi.*
import org.jetbrains.uast.*

object JavaConverter : UastConverter {
    override fun isFileSupported(path: String): Boolean {
        return path.endsWith(".java", ignoreCase = true)
    }

    fun convert(file: PsiJavaFile): UFile = JavaUFile(file)

    override fun convert(element: Any?, parent: UElement): UElement? {
        if (element !is PsiElement) return null
        return convertPsiElement(element, parent)
    }

    override fun convertWithParent(element: Any?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiJavaFile) return JavaUFile(element)

        val parent = element.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null
        return convertPsiElement(element, parentUElement)
    }

    fun convertAnnotation(annotation: PsiAnnotation) = convertWithParent(annotation) as UAnnotation

    private fun convertPsiElement(element: PsiElement?, parent: UElement) = when (element) {
        is PsiJavaFile -> JavaUFile(element)
        is PsiClass -> JavaUClass(element, parent)
        is PsiCodeBlock -> convert(element, parent)
        is PsiMethod -> convert(element, parent)
        is PsiField -> convert(element, parent)
        is PsiVariable -> convert(element, parent)
        is PsiClassInitializer -> convert(element, parent)
        is PsiAnnotation -> convert(element, parent)
        is PsiExpression -> convert(element, parent)
        is PsiStatement -> convert(element, parent)
        is PsiIdentifier -> JavaUSimpleReferenceExpression(element, element.text, parent)
        is PsiImportStatementBase -> convert(element, parent)
        is PsiParameter -> convert(element, parent)
        is PsiTypeParameter -> convert(element, parent)
        is PsiNameValuePair -> convert(element, parent)
        is PsiType -> convert(element, parent)
        is PsiArrayInitializerMemberValue -> JavaAnnotationArrayInitializerUCallExpression(element, parent)
        else -> null
    }

    internal fun convert(importStatement: PsiImportStatementBase, parent: UElement): UImportStatement? = when (importStatement) {
        is PsiImportStatement -> JavaUImportStatement(importStatement, parent)
        is PsiImportStaticStatement -> JavaUStaticImportStatement(importStatement, parent)
        else -> null
    }

    internal fun convert(type: PsiType?, parent: UElement) = JavaUType(type, parent)

    internal fun convert(parameter: PsiParameter, parent: UElement) = JavaValueParameterUVariable(parameter, parent)

    internal fun convert(block: PsiCodeBlock, parent: UElement) = JavaUCodeBlockExpression(block, parent)

    internal fun convert(method: PsiMethod, parent: UElement) = JavaUFunction(method, parent)

    internal fun convert(field: PsiField, parent: UElement) = JavaUVariable(field, parent)

    internal fun convert(variable: PsiVariable, parent: UElement) = JavaUVariable(variable, parent)

    internal fun convert(annotation: PsiAnnotation, parent: UElement) = JavaUAnnotation(annotation, parent)

    internal fun convert(clazz: PsiClass, parent: UElement) = JavaUClass(clazz, parent)

    internal fun convert(initializer: PsiClassInitializer, parent: UElement) = JavaClassInitializerUFunction(initializer, parent)

    internal fun convert(parameter: PsiTypeParameter, parent: UElement) = JavaParameterUTypeReference(parameter, parent)

    internal fun convert(pair: PsiNameValuePair, parent: UElement) = UNamedExpression(pair.name.orAnonymous(), parent).apply {
        val value = pair.value
        expression = convert(value, this) as? UExpression ?: UnknownJavaExpression(value ?: pair, this)
    }

    internal fun convert(expression: PsiReferenceExpression, parent: UElement): UExpression {
        return if (expression.isQualified) {
            JavaUQualifiedExpression(expression, parent)
        } else {
            val name = expression.referenceName ?: "<error name>"
            val element = expression.referenceNameElement ?: expression
            JavaUSimpleReferenceExpression(element, name, parent)
        }
    }

    internal fun convert(expression: PsiQualifiedReferenceElement, parent: UElement): UExpression {
        val referenceName = expression.referenceName ?: "<error name>"
        val referenceNameElement = expression.element ?: expression

        return JavaUCompositeQualifiedExpression(parent).apply {
            receiver = expression.qualifier?.let { convert(it, this) } as? UExpression ?: EmptyExpression(parent)
            selector = JavaUSimpleReferenceExpression(referenceNameElement, referenceName, this)
        }
    }

    private fun convertPolyadicExpression(
            expression: PsiPolyadicExpression,
            parent: UElement,
            i: Int
    ): UExpression {
        return if (i == 1) JavaCombinedUBinaryExpression(expression, parent).apply {
            leftOperand = convert(expression.operands[0], this)
            rightOperand = convert(expression.operands[1], this)
        } else JavaCombinedUBinaryExpression(expression, parent).apply {
            leftOperand = convertPolyadicExpression(expression, parent, i - 1)
            rightOperand = convert(expression.operands[i], this)
        }
    }

    internal fun convert(expression: PsiExpression, parent: UElement): UExpression = when (expression) {
        is PsiPolyadicExpression -> convertPolyadicExpression(expression, parent, expression.operands.size - 1)
        is PsiAssignmentExpression -> JavaUAssignmentExpression(expression, parent)
        is PsiConditionalExpression -> JavaUTernaryIfExpression(expression, parent)
        is PsiNewExpression -> {
            if (expression.anonymousClass != null) {
                JavaUObjectLiteralExpression(expression, parent)
            } else {
                JavaConstructorUCallExpression(expression, parent)
            }
        }
        is PsiMethodCallExpression -> {
            val qualifier = expression.methodExpression.qualifierExpression
            if (qualifier != null) {
                JavaUCompositeQualifiedExpression(parent).apply {
                    receiver = convert(qualifier, this)
                    selector = JavaUCallExpression(expression, this)
                }
            } else {
                JavaUCallExpression(expression, parent)
            }
        }
        is PsiArrayInitializerExpression -> JavaArrayInitializerUCallExpression(expression, parent)
        is PsiBinaryExpression -> JavaUBinaryExpression(expression, parent)
        is PsiParenthesizedExpression -> JavaUParenthesizedExpression(expression, parent)
        is PsiPrefixExpression -> JavaUPrefixExpression(expression, parent)
        is PsiPostfixExpression -> JavaUPostfixExpression(expression, parent)
        is PsiLiteralExpression -> JavaULiteralExpression(expression, parent)
        is PsiReferenceExpression -> convert(expression, parent)
        is PsiThisExpression -> JavaUThisExpression(expression, parent)
        is PsiSuperExpression -> JavaUSuperExpression(expression, parent)
        is PsiInstanceOfExpression -> JavaUInstanceCheckExpression(expression, parent)
        is PsiTypeCastExpression -> JavaUTypeCastExpression(expression, parent)
        is PsiClassObjectAccessExpression -> JavaUClassLiteralExpression(expression, parent)
        is PsiArrayAccessExpression -> JavaUArrayAccessExpression(expression, parent)
        is PsiLambdaExpression -> JavaULambdaExpression(expression, parent)
        is PsiMethodReferenceExpression -> JavaUCallableReferenceExpression(expression, parent)

        else -> UnknownJavaExpression(expression, parent)
    }

    internal fun convert(statement: PsiStatement, parent: UElement): UExpression = when (statement) {
        is PsiDeclarationStatement -> convertDeclarations(statement.declaredElements, parent)
        is PsiExpressionListStatement -> convertDeclarations(statement.expressionList.expressions, parent)
        is PsiBlockStatement -> JavaUBlockExpression(statement, parent)
        is PsiLabeledStatement -> JavaULabeledExpression(statement, parent)
        is PsiExpressionStatement -> convert(statement.expression, parent)
        is PsiIfStatement -> JavaUIfExpression(statement, parent)
        is PsiSwitchStatement -> JavaUSwitchExpression(statement, parent)
        is PsiSwitchLabelStatement -> {
            if (statement.isDefaultCase)
                SimpleUDefaultSwitchClauseExpression(parent)
            else JavaUExpressionSwitchClauseExpression(statement, parent)
        }
        is PsiWhileStatement -> JavaUWhileExpression(statement, parent)
        is PsiDoWhileStatement -> JavaUDoWhileExpression(statement, parent)
        is PsiForStatement -> JavaUForExpression(statement, parent)
        is PsiForeachStatement -> JavaUForEachExpression(statement, parent)
        is PsiBreakStatement -> JavaUSpecialExpressionList.Empty(statement, UastSpecialExpressionKind.BREAK, parent)
        is PsiContinueStatement -> JavaUSpecialExpressionList.Empty(statement, UastSpecialExpressionKind.CONTINUE, parent)
        is PsiReturnStatement -> JavaUSpecialExpressionList(statement, UastSpecialExpressionKind.RETURN, parent).apply {
            expressions = singletonListOrEmpty(convertOrNull(statement.returnValue, this))
        }
        is PsiAssertStatement -> JavaUSpecialExpressionList(statement, JavaSpecialExpressionKinds.ASSERT, parent).apply {
            expressions = listOf(
                    convertOrEmpty(statement.assertCondition, this),
                    convertOrEmpty(statement.assertDescription, this))
        }
        is PsiThrowStatement -> JavaUSpecialExpressionList(statement, UastSpecialExpressionKind.THROW, parent).apply {
            expressions = singletonListOrEmpty(convertOrNull(statement.exception, this))
        }
        is PsiSynchronizedStatement -> JavaUSpecialExpressionList(statement, JavaSpecialExpressionKinds.SYNCHRONIZED, parent).apply {
            expressions = listOf(
                    convertOrEmpty(statement.lockExpression, this),
                    convertOrEmpty(statement.body, this))
        }
        is PsiTryStatement -> JavaUTryExpression(statement, parent)

        else -> UnknownJavaExpression(statement, parent)
    }

    internal fun convertOrEmpty(statement: PsiStatement?, parent: UElement): UExpression {
        return if (statement != null) convert(statement, parent) else EmptyExpression(parent)
    }

    internal fun convertOrEmpty(expression: PsiExpression?, parent: UElement): UExpression {
        return if (expression != null) convert(expression, parent) else EmptyExpression(parent)
    }

    internal fun convertOrNull(expression: PsiExpression?, parent: UElement): UExpression? {
        return if (expression != null) convert(expression, parent) else null
    }

    internal fun convertOrEmpty(block: PsiCodeBlock?, parent: UElement): UExpression {
        return if (block != null) convert(block, parent) else EmptyExpression(parent)
    }

    private fun convertDeclarations(elements: Array<out PsiElement>, parent: UElement): SimpleUDeclarationsExpression {
        val uelements = arrayListOf<UElement>()
        return SimpleUDeclarationsExpression(parent, uelements).apply {
            for (element in elements) {
                convert(element, this)?.let { uelements += it }
            }
        }
    }
}