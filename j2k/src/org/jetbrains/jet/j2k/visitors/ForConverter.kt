/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.PsiForStatement
import org.jetbrains.jet.j2k.ast.ForeachStatement
import com.intellij.psi.PsiDeclarationStatement
import org.jetbrains.jet.j2k.singleOrNull2
import com.intellij.psi.PsiLocalVariable
import org.jetbrains.jet.j2k.hasWriteAccesses
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiExpressionStatement
import org.jetbrains.jet.j2k.ast.PrimitiveType
import org.jetbrains.jet.j2k.ast.Identifier
import org.jetbrains.jet.j2k.ast.assignNoPrototype
import org.jetbrains.jet.j2k.ast.declarationIdentifier
import org.jetbrains.jet.j2k.isInSingleLine
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiPostfixExpression
import com.intellij.psi.PsiPrefixExpression
import com.intellij.psi.PsiExpression
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.j2k.ast.Expression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.CommonClassNames
import org.jetbrains.jet.j2k.ast.QualifiedExpression
import com.intellij.psi.PsiArrayType
import org.jetbrains.jet.j2k.ast.BinaryExpression
import org.jetbrains.jet.j2k.ast.LiteralExpression
import org.jetbrains.jet.j2k.ast.RangeExpression
import org.jetbrains.jet.j2k.Converter
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.j2k.ast.Block
import org.jetbrains.jet.j2k.ast.LBrace
import org.jetbrains.jet.j2k.ast.RBrace
import org.jetbrains.jet.j2k.ast.assignPrototypesFrom
import org.jetbrains.jet.j2k.ast.WhileStatement
import org.jetbrains.jet.j2k.ast.MethodCallExpression
import org.jetbrains.jet.j2k.ast.LambdaExpression
import org.jetbrains.jet.j2k.ast.Statement

class ForConverter(private val statement: PsiForStatement, private val converter: Converter) {
    private val initialization = statement.getInitialization()
    private val update = statement.getUpdate()
    private val condition = statement.getCondition()
    private val body = statement.getBody()

    public fun execute(): Statement {
        val foreach = convertToForeach()
        if (foreach != null) return foreach

        val initialization = statement.getInitialization()
        val update = statement.getUpdate()
        val condition = statement.getCondition()
        val body = statement.getBody()

        val initializationConverted = converter.convertStatement(initialization)
        val updateConverted = converter.convertStatement(update)

        val whileBody = if (updateConverted.isEmpty) {
            converter.convertStatementOrBlock(body)
        }
        else if (body is PsiBlockStatement) {
            val nameConflict = initialization is PsiDeclarationStatement && initialization.getDeclaredElements().any { loopVar ->
                loopVar is PsiNamedElement && body.getCodeBlock().getStatements().any { statement ->
                    statement is PsiDeclarationStatement && statement.getDeclaredElements().any {
                        it is PsiNamedElement && it.getName() == loopVar.getName()
                    }
                }
            }

            if (nameConflict) {
                val statements = listOf(converter.convertStatement(body), updateConverted)
                Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
            }
            else {
                val block = converter.convertBlock(body.getCodeBlock(), true)
                Block(block.statements + listOf(updateConverted), block.lBrace, block.rBrace, true).assignPrototypesFrom(block)
            }
        }
        else {
            val statements = listOf(converter.convertStatement(body), updateConverted)
            Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
        }

        val whileStatement = WhileStatement(
                if (condition != null) converter.convertExpression(condition) else LiteralExpression("true").assignNoPrototype(),
                whileBody,
                statement.isInSingleLine()).assignNoPrototype()
        if (initializationConverted.isEmpty) return whileStatement

        val statements = listOf(initializationConverted, whileStatement)
        val block = Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
        return MethodCallExpression.build(null, "run", listOf(), listOf(), false, LambdaExpression(null, block))
    }

    private fun convertToForeach(): ForeachStatement? {
        if (initialization is PsiDeclarationStatement) {
            val loopVar = initialization.getDeclaredElements().singleOrNull2() as? PsiLocalVariable
            if (loopVar != null
                    && !loopVar.hasWriteAccesses(body)
                    && !loopVar.hasWriteAccesses(condition)
                    && condition is PsiBinaryExpression) {
                val operationTokenType = condition.getOperationTokenType()
                val lowerBound = condition.getLOperand()
                val upperBound = condition.getROperand()
                if ((operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE) &&
                        lowerBound is PsiReferenceExpression &&
                        lowerBound.resolve() == loopVar &&
                        upperBound != null) {
                    val start = loopVar.getInitializer()
                    if (start != null &&
                            (update as? PsiExpressionStatement)?.getExpression()?.isVariablePlusPlus(loopVar) ?: false) {
                        val range = forIterationRange(start, upperBound, operationTokenType).assignNoPrototype()
                        val explicitType = if (converter.settings.specifyLocalVariableTypeByDefault)
                            PrimitiveType(Identifier("Int").assignNoPrototype()).assignNoPrototype()
                        else
                            null
                        return ForeachStatement(loopVar.declarationIdentifier(), explicitType, range, converter.convertStatementOrBlock(body), statement.isInSingleLine())
                    }
                }
            }
        }
        return null
    }

    private fun PsiElement.isVariablePlusPlus(variable: PsiVariable): Boolean {
        //TODO: simplify code when KT-5453 fixed
        val pair = when (this) {
            is PsiPostfixExpression -> getOperationTokenType() to getOperand()
            is PsiPrefixExpression -> getOperationTokenType() to getOperand()
            else -> return false
        }
        return pair.first == JavaTokenType.PLUSPLUS && (pair.second as? PsiReferenceExpression)?.resolve() == variable
    }

    private fun forIterationRange(start: PsiExpression, upperBound: PsiExpression, comparisonTokenType: IElementType): Expression {
        if (start is PsiLiteralExpression
                && start.getValue() == 0
                && comparisonTokenType == JavaTokenType.LT) {
            // check if it's iteration through list indices
            if (upperBound is PsiMethodCallExpression && upperBound.getArgumentList().getExpressions().isEmpty()) {
                val methodExpr = upperBound.getMethodExpression()
                if (methodExpr is PsiReferenceExpression && methodExpr.getReferenceName() == "size") {
                    val qualifier = methodExpr.getQualifierExpression()
                    if (qualifier is PsiReferenceExpression /* we don't convert to .indices if qualifier is method call or something because of possible side effects */) {
                        val listType = PsiElementFactory.SERVICE.getInstance(converter.project).createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST)
                        val qualifierType = qualifier.getType()
                        if (qualifierType != null && listType.isAssignableFrom(qualifierType)) {
                            return QualifiedExpression(converter.convertExpression(qualifier), Identifier("indices", false).assignNoPrototype())
                        }
                    }
                }
            }

            // check if it's iteration through array indices
            if (upperBound is PsiReferenceExpression /* we don't convert to .indices if qualifier is method call or something because of possible side effects */
                    && upperBound.getReferenceName() == "length") {
                val qualifier = upperBound.getQualifierExpression()
                if (qualifier is PsiReferenceExpression && qualifier.getType() is PsiArrayType) {
                    return QualifiedExpression(converter.convertExpression(qualifier), Identifier("indices", false).assignNoPrototype())
                }
            }
        }

        val end = converter.convertExpression(upperBound)
        val endExpression = if (comparisonTokenType == JavaTokenType.LT)
            BinaryExpression(end, LiteralExpression("1").assignNoPrototype(), "-").assignNoPrototype()
        else
            end
        return RangeExpression(converter.convertExpression(start), endExpression)
    }

}