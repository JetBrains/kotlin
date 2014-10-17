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

package org.jetbrains.jet.j2k

import com.intellij.psi.PsiType
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiStatement
import org.jetbrains.jet.j2k.ast.Block
import org.jetbrains.jet.j2k.ast.LBrace
import org.jetbrains.jet.j2k.ast.assignPrototype
import org.jetbrains.jet.j2k.ast.RBrace
import org.jetbrains.jet.j2k.ast.Statement
import com.intellij.psi.PsiExpression
import org.jetbrains.jet.j2k.ast.Expression
import com.intellij.psi.PsiLocalVariable
import org.jetbrains.jet.j2k.ast.LocalVariable
import com.intellij.psi.PsiModifier
import org.jetbrains.jet.j2k.ast.declarationIdentifier
import org.jetbrains.jet.j2k.ast.Identifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiClassType
import org.jetbrains.jet.j2k.ast.BangBangExpression
import org.jetbrains.jet.j2k.ast.assignNoPrototype
import org.jetbrains.jet.j2k.ast.LiteralExpression
import org.jetbrains.jet.j2k.ast.MethodCallExpression
import org.jetbrains.jet.j2k.ast.Type
import org.jetbrains.jet.j2k.ast.ErrorType
import org.jetbrains.jet.j2k.ast.Nullability
import com.intellij.psi.CommonClassNames.JAVA_LANG_BYTE
import com.intellij.psi.CommonClassNames.JAVA_LANG_SHORT
import com.intellij.psi.CommonClassNames.JAVA_LANG_INTEGER
import com.intellij.psi.CommonClassNames.JAVA_LANG_LONG
import com.intellij.psi.CommonClassNames.JAVA_LANG_FLOAT
import com.intellij.psi.CommonClassNames.JAVA_LANG_DOUBLE
import com.intellij.psi.CommonClassNames.JAVA_LANG_CHARACTER
import com.intellij.psi.PsiAnonymousClass
import org.jetbrains.jet.j2k.ast.AnonymousClassBody

class CodeConverter(public val converter: Converter,
                    private val expressionConverter: ExpressionConverter,
                    private val statementConverter: StatementConverter,
                    public val methodReturnType: PsiType?) {

    public val typeConverter: TypeConverter = converter.typeConverter
    public val settings: ConverterSettings = converter.settings

    public fun withSpecialExpressionConverter(specialConverter: SpecialExpressionConverter): CodeConverter
            = CodeConverter(converter, expressionConverter.withSpecialConverter(specialConverter), statementConverter, methodReturnType)

    public fun withMethodReturnType(methodReturnType: PsiType?): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    public fun withConverter(converter: Converter): CodeConverter
            = CodeConverter(converter, expressionConverter, statementConverter, methodReturnType)

    public fun convertBlock(block: PsiCodeBlock?, notEmpty: Boolean = true, statementFilter: (PsiStatement) -> Boolean = { true }): Block {
        if (block == null) return Block.Empty()

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
        if (expression == null) return Identifier.Empty()

        var convertedExpression = convertExpression(expression)
        if (expectedType == null || expectedType == PsiType.VOID) return convertedExpression

        val actualType = expression.getType()
        if (actualType == null) return convertedExpression

        if (convertedExpression.isNullable &&
            (actualType is PsiPrimitiveType || actualType is PsiClassType && expectedType is PsiPrimitiveType)) {
            convertedExpression = BangBangExpression(convertedExpression).assignNoPrototype()
        }

        if (needConversion(actualType, expectedType) && convertedExpression !is LiteralExpression) {
            val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType.getCanonicalText()]
            if (conversion != null) {
                convertedExpression = MethodCallExpression.buildNotNull(convertedExpression, conversion)
            }
        }

        return convertedExpression.assignPrototype(expression)
    }

    public fun convertedExpressionType(expression: PsiExpression, expectedType: PsiType): Type {
        var convertedExpression = convertExpression(expression)
        val actualType = expression.getType()
        if (actualType == null) return ErrorType()
        var resultType = typeConverter.convertType(actualType, if (convertedExpression.isNullable) Nullability.Nullable else Nullability.NotNull)

        if (actualType is PsiPrimitiveType && resultType.isNullable ||
            expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            resultType = resultType.toNotNullType()
        }

        if (needConversion(actualType, expectedType) && convertedExpression !is LiteralExpression) {
            val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType.getCanonicalText()]
            if (conversion != null) {
                resultType = typeConverter.convertType(expectedType, Nullability.NotNull)
            }
        }

        return resultType
    }

    public fun convertAnonymousClassBody(anonymousClass: PsiAnonymousClass): AnonymousClassBody {
        return AnonymousClassBody(ClassBodyConverter(anonymousClass, converter).convertBody(),
                                  anonymousClass.getBaseClassType().resolve()?.isInterface() ?: false).assignPrototype(anonymousClass)
    }

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