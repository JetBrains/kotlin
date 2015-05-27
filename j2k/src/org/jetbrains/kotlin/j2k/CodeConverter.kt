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

class CodeConverter(
        public val converter: Converter,
        private val expressionConverter: ExpressionConverter,
        private val statementConverter: StatementConverter,
        public val methodReturnType: PsiType?
) {

    public val typeConverter: TypeConverter = converter.typeConverter
    public val settings: ConverterSettings = converter.settings

    public fun withSpecialExpressionConverter(specialConverter: SpecialExpressionConverter): CodeConverter
            = CodeConverter(converter, expressionConverter.withSpecialConverter(specialConverter), statementConverter, methodReturnType)

    public fun withSpecialStatementConverter(specialConverter: SpecialStatementConverter): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter.withSpecialConverter(specialConverter), methodReturnType)

    public fun withMethodReturnType(methodReturnType: PsiType?): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    public fun withConverter(converter: Converter): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    public fun convertBlock(block: PsiCodeBlock?, notEmpty: Boolean = true, statementFilter: (PsiStatement) -> Boolean = { true }): Block {
        if (block == null) return Block.Empty

        val lBrace = LBrace().assignPrototype(block.getLBrace())
        val rBrace = RBrace().assignPrototype(block.getRBrace())
        return Block(block.getStatements().filter(statementFilter).map { convertStatement(it) }, lBrace, rBrace, notEmpty).assignPrototype(block)
    }

    public fun convertStatement(statement: PsiStatement?): Statement {
        if (statement == null) return Statement.Empty

        return statementConverter.convertStatement(statement, this).assignPrototype(statement)
    }

    public fun convertExpressions(expressions: Array<PsiExpression>): List<Expression>
            = expressions.map { convertExpression(it) }

    public fun convertExpression(expression: PsiExpression?): Expression {
        if (expression == null) return Expression.Empty

        return expressionConverter.convertExpression(expression, this).assignPrototype(expression)
    }

    public fun convertLocalVariable(variable: PsiLocalVariable): LocalVariable {
        val isVal = variable.hasModifierProperty(PsiModifier.FINAL) ||
                    variable.getInitializer() == null/* we do not know actually and prefer val until we have better analysis*/ ||
                    !variable.hasWriteAccesses(converter.referenceSearcher, variable.getContainingMethod())
        return LocalVariable(variable.declarationIdentifier(),
                             converter.convertAnnotations(variable),
                             converter.convertModifiers(variable),
                             converter.variableTypeToDeclare(variable, settings.specifyLocalVariableTypeByDefault, isVal),
                             convertExpression(variable.getInitializer(), variable.getType()),
                             isVal).assignPrototype(variable)
    }

    public fun convertExpression(expression: PsiExpression?, expectedType: PsiType?): Expression {
        if (expression == null) return Identifier.Empty

        var convertedExpression = convertExpression(expression)
        if (expectedType == null || expectedType == PsiType.VOID) return convertedExpression

        val actualType = expression.getType() ?: return convertedExpression

        if (actualType is PsiPrimitiveType || actualType is PsiClassType && expectedType is PsiPrimitiveType) {
            convertedExpression = BangBangExpression.surroundIfNullable(convertedExpression)
        }

        if (needConversion(actualType, expectedType)) {
            val expectedTypeStr = expectedType.getCanonicalText()
            if (expression is PsiLiteralExpression) {
                if (expectedTypeStr == "float" || expectedTypeStr == "double") {
                    var text = convertedExpression.canonicalCode()
                    if (text.last() in setOf('f', 'L')) {
                        text = text.substring(0, text.length() - 1)
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
            }
            else if (expression is PsiPrefixExpression && expression.isLiteralWithSign()) {
                val operandConverted = convertExpression(expression.getOperand(), expectedType)
                convertedExpression = PrefixExpression(expression.getOperationSign().getText(), operandConverted)
            }
            else {
                val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedTypeStr]
                if (conversion != null) {
                    convertedExpression = MethodCallExpression.buildNotNull(convertedExpression, conversion)
                }
            }
        }

        return convertedExpression.assignPrototype(expression)
    }

    public fun convertedExpressionType(expression: PsiExpression, expectedType: PsiType): Type {
        var convertedExpression = convertExpression(expression)
        val actualType = expression.getType() ?: return ErrorType()
        var resultType = typeConverter.convertType(actualType, if (convertedExpression.isNullable) Nullability.Nullable else Nullability.NotNull)

        if (actualType is PsiPrimitiveType && resultType.isNullable ||
            expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            resultType = resultType.toNotNullType()
        }

        if (needConversion(actualType, expectedType)) {
            val expectedTypeStr = expectedType.getCanonicalText()

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
            = getOperand() is PsiLiteralExpression && getOperationTokenType() in setOf(JavaTokenType.PLUS, JavaTokenType.MINUS)

    private fun needConversion(actual: PsiType, expected: PsiType): Boolean {
        val expectedStr = expected.getCanonicalText()
        val actualStr = actual.getCanonicalText()
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
