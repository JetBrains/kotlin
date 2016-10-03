/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.utils.addToStdlib.check

class CodeConverter(
        val converter: Converter,
        private val expressionConverter: ExpressionConverter,
        private val statementConverter: StatementConverter,
        val methodReturnType: PsiType?
) {

    val typeConverter: TypeConverter = converter.typeConverter
    val settings: ConverterSettings = converter.settings

    fun withSpecialExpressionConverter(specialConverter: SpecialExpressionConverter): CodeConverter
            = CodeConverter(converter, expressionConverter.withSpecialConverter(specialConverter), statementConverter, methodReturnType)

    fun withSpecialStatementConverter(specialConverter: SpecialStatementConverter): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter.withSpecialConverter(specialConverter), methodReturnType)

    fun withMethodReturnType(methodReturnType: PsiType?): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    fun withConverter(converter: Converter): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    fun convertBlock(block: PsiCodeBlock?, notEmpty: Boolean = true, statementFilter: (PsiStatement) -> Boolean = { true }): Block {
        if (block == null) return Block.Empty

        val lBrace = LBrace().assignPrototype(block.lBrace)
        val rBrace = RBrace().assignPrototype(block.rBrace)
        return Block(block.statements.filter(statementFilter).map { convertStatement(it) }, lBrace, rBrace, notEmpty).assignPrototype(block)
    }

    fun convertStatement(statement: PsiStatement?): Statement {
        if (statement == null) return Statement.Empty

        return statementConverter.convertStatement(statement, this).assignPrototype(statement)
    }

    fun convertExpressionsInList(expressions: List<PsiExpression>): List<Expression>
            = expressions.map { convertExpression(it).assignPrototype(it, CommentsAndSpacesInheritance.LINE_BREAKS) }

    fun convertArgumentList(list: PsiExpressionList): ArgumentList {
        return ArgumentList(
                convertExpressionsInList(list.expressions.asList()),
                LPar.withPrototype(list.lPar()),
                RPar.withPrototype(list.rPar())
        ).assignPrototype(list)
    }

    fun convertExpression(expression: PsiExpression?, shouldParenthesize: Boolean = false): Expression {
        if (expression == null) return Expression.Empty

        val converted = expressionConverter.convertExpression(expression, this).assignPrototype(expression)
        if (shouldParenthesize) {
            return ParenthesizedExpression(converted).assignNoPrototype()
        }
        return converted
    }

    fun convertLocalVariable(variable: PsiLocalVariable): LocalVariable {
        val isVal = canChangeType(variable)
        val type = typeConverter.convertVariableType(variable)
        val explicitType = type.check { settings.specifyLocalVariableTypeByDefault || converter.shouldDeclareVariableType(variable, type, isVal) }
        return LocalVariable(variable.declarationIdentifier(),
                             converter.convertAnnotations(variable),
                             converter.convertModifiers(variable, false),
                             explicitType,
                             convertExpression(variable.initializer, variable.type),
                             isVal).assignPrototype(variable)
    }

    fun canChangeType(variable: PsiLocalVariable) : Boolean {
        return variable.hasModifierProperty(PsiModifier.FINAL) ||
                    variable.initializer == null/* we do not know actually and prefer val until we have better analysis*/ ||
                    !variable.hasWriteAccesses(converter.referenceSearcher, variable.getContainingMethod())
    }

    fun convertExpression(expression: PsiExpression?, expectedType: PsiType?, expectedNullability: Nullability? = null): Expression {
        if (expression == null) return Identifier.Empty

        var convertedExpression = convertExpression(expression)

        if (convertedExpression.isNullable && expectedNullability != null && expectedNullability == Nullability.NotNull) {
            convertedExpression = BangBangExpression.surroundIfNullable(convertedExpression)
        }

        if (expectedType == null || expectedType == PsiType.VOID) return convertedExpression

        val actualType = expression.type ?: return convertedExpression

        if (actualType is PsiPrimitiveType || actualType is PsiClassType && expectedType is PsiPrimitiveType) {
            convertedExpression = BangBangExpression.surroundIfNullable(convertedExpression)
        }

        if (needConversion(actualType, expectedType)) {
            val expectedTypeStr = expectedType.canonicalText
            if (expression is PsiLiteralExpression) {
                if (expectedTypeStr == "float" || expectedTypeStr == "double") {
                    var text = convertedExpression.canonicalCode()
                    if (text.last() in setOf('f', 'L')) {
                        text = text.substring(0, text.length - 1)
                    }
                    if (expectedTypeStr == "float") {
                        text += "f"
                    }
                    else {
                        if (!text.contains(".")) {
                            text += ".0"
                        }
                    }
                    convertedExpression = LiteralExpression(text)
                }
                else if (expectedTypeStr == "char") {
                    convertedExpression = MethodCallExpression.buildNonNull(convertedExpression, "toChar")
                }
            }
            else if (expression is PsiPrefixExpression && expression.isLiteralWithSign()) {
                val operandConverted = convertExpression(expression.operand, expectedType)
                convertedExpression = PrefixExpression(Operator(expression.operationSign.tokenType).assignPrototype(expression.operationSign), operandConverted)
            }
            else {
                val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedTypeStr]
                if (conversion != null) {
                    convertedExpression = MethodCallExpression.buildNonNull(convertedExpression, conversion)
                }
            }
        }

        return convertedExpression.assignPrototype(expression)
    }

    fun convertedExpressionType(expression: PsiExpression, expectedType: PsiType): Type {
        val convertedExpression = convertExpression(expression)
        val actualType = expression.type ?: return ErrorType()
        var resultType = typeConverter.convertType(actualType, if (convertedExpression.isNullable) Nullability.Nullable else Nullability.NotNull)

        if (actualType is PsiPrimitiveType && resultType.isNullable ||
            expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            resultType = resultType.toNotNullType()
        }

        if (needConversion(actualType, expectedType)) {
            val expectedTypeStr = expectedType.canonicalText

            val willConvert = if (convertedExpression is LiteralExpression
                                  || expression is PsiPrefixExpression && expression.isLiteralWithSign() )
                expectedTypeStr == "float" || expectedTypeStr == "double"
            else
                PRIMITIVE_TYPE_CONVERSIONS[expectedTypeStr] != null

            if (willConvert) {
                resultType = typeConverter.convertType(expectedType, Nullability.NotNull)
            }
        }

        return resultType
    }

    private fun PsiPrefixExpression.isLiteralWithSign()
            = operand is PsiLiteralExpression && operationTokenType in setOf(JavaTokenType.PLUS, JavaTokenType.MINUS)

    private fun needConversion(actual: PsiType, expected: PsiType): Boolean {
        val expectedStr = expected.canonicalText
        val actualStr = actual.canonicalText
        return expectedStr != actualStr &&
               expectedStr != typeConversionMap[actualStr] &&
               actualStr != typeConversionMap[expectedStr]
    }

    private val typeConversionMap: Map<String, String> = mapOf(
            JAVA_LANG_BYTE to "byte",
            JAVA_LANG_SHORT to "short",
            JAVA_LANG_INTEGER to "int",
            JAVA_LANG_LONG to "long",
            JAVA_LANG_FLOAT to "float",
            JAVA_LANG_DOUBLE to "double",
            JAVA_LANG_CHARACTER to "char"
    )
}
