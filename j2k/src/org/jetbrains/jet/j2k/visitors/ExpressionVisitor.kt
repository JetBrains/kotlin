/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import java.util.ArrayList
import com.intellij.psi.CommonClassNames.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.types.lang.PrimitiveType
import org.jetbrains.jet.j2k.*

open class ExpressionVisitor(protected val converter: Converter,
                             private val usageReplacementMap: Map<PsiVariable, String> = mapOf()) : JavaElementVisitor() {
    public var result: Expression = Expression.Empty
        protected set

    override fun visitArrayAccessExpression(expression: PsiArrayAccessExpression) {
        val assignment = PsiTreeUtil.getParentOfType(expression, javaClass<PsiAssignmentExpression>())
        val lvalue = assignment != null && expression == assignment.getLExpression();
        result = ArrayAccessExpression(converter.convertExpression(expression.getArrayExpression()),
                                       converter.convertExpression(expression.getIndexExpression()),
                                         lvalue)
    }

    override fun visitArrayInitializerExpression(expression: PsiArrayInitializerExpression) {
        val expressionType = converter.convertType(expression.getType())
        assert(expressionType is ArrayType) { "Array initializer must have array type" }
        result = ArrayInitializerExpression(expressionType as ArrayType,
                                              converter.convertExpressions(expression.getInitializers()))
    }

    override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
        val tokenType = expression.getOperationSign().getTokenType()
        val secondOp = when(tokenType) {
            JavaTokenType.GTGTEQ -> "shr"
            JavaTokenType.LTLTEQ -> "shl"
            JavaTokenType.XOREQ -> "xor"
            JavaTokenType.ANDEQ -> "and"
            JavaTokenType.OREQ -> "or"
            JavaTokenType.GTGTGTEQ -> "ushr"
            else -> ""
        }

        val lhs = converter.convertExpression(expression.getLExpression())
        val rhs = converter.convertExpression(expression.getRExpression()!!, expression.getLExpression().getType())
        if (!secondOp.isEmpty()) {
            result = AssignmentExpression(lhs, BinaryExpression(lhs, rhs, secondOp), "=")
        }
        else {
            result = AssignmentExpression(lhs, rhs, expression.getOperationSign().getText()!!)
        }
    }

    override fun visitBinaryExpression(expression: PsiBinaryExpression) {
        val lhs = converter.convertExpression(expression.getLOperand(), expression.getType())
        val rhs = converter.convertExpression(expression.getROperand(), expression.getType())
        if (expression.getOperationSign().getTokenType() == JavaTokenType.GTGTGT) {
            result = MethodCallExpression.build(lhs, "ushr", listOf(rhs))
        }
        else {
            result = BinaryExpression(lhs, rhs,
                                        getOperatorString(expression.getOperationSign().getTokenType()))
        }
    }

    override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
        result = ClassObjectAccessExpression(converter.convertTypeElement(expression.getOperand()))
    }

    override fun visitConditionalExpression(expression: PsiConditionalExpression) {
        val condition = expression.getCondition()
        val `type` = condition.getType()
        val e = if (`type` != null)
            converter.convertExpression(condition, `type`)
        else
            converter.convertExpression(condition)
        result = ParenthesizedExpression(IfStatement(e,
                                                       converter.convertExpression(expression.getThenExpression()),
                                                       converter.convertExpression(expression.getElseExpression())))
    }

    override fun visitExpressionList(list: PsiExpressionList) {
        result = ExpressionList(converter.convertExpressions(list.getExpressions()))
    }

    override fun visitInstanceOfExpression(expression: PsiInstanceOfExpression) {
        val checkType = expression.getCheckType()
        result = IsOperator(converter.convertExpression(expression.getOperand()),
                              converter.convertTypeElement(checkType))
    }

    override fun visitLiteralExpression(expression: PsiLiteralExpression) {
        val value = expression.getValue()
        var text = expression.getText()!!
        val `type` = expression.getType()
        if (`type` != null) {
            val canonicalTypeStr = `type`.getCanonicalText()
            if (canonicalTypeStr == "double" || canonicalTypeStr == JAVA_LANG_DOUBLE) {
                text = text.replace("D", "").replace("d", "")
                if (!text.contains(".")) {
                    text += ".0"
                }

            }

            if (canonicalTypeStr == "float" || canonicalTypeStr == JAVA_LANG_FLOAT) {
                text = text.replace("F", "").replace("f", "") + "." + OperatorConventions.FLOAT + "()"
            }

            if (canonicalTypeStr == "long" || canonicalTypeStr == JAVA_LANG_LONG) {
                text = text.replace("L", "").replace("l", "")
            }

            if (canonicalTypeStr == "int" || canonicalTypeStr == JAVA_LANG_INTEGER) {
                text = if (value != null) value.toString() else text
            }
        }

        result = LiteralExpression(text)
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        convertMethodCallExpression(expression)
    }

    protected fun convertMethodCallExpression(expression: PsiMethodCallExpression) {
        if (!expression.isSuperConstructorCall() || !expression.isInsidePrimaryConstructor()) {
            result = MethodCallExpression(converter.convertExpression(expression.getMethodExpression()),
                                            converter.convertArguments(expression),
                                            converter.convertTypes(expression.getTypeArguments()),
                                            converter.convertType(expression.getType()).isNullable)
        }
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        if (expression.getArrayInitializer() != null) {
            result = createNewEmptyArray(expression)
        }
        else if (expression.getArrayDimensions().size > 0) {
            result = createNewEmptyArrayWithoutInitialization(expression)
        }
        else {
            result = createNewClassExpression(expression)
        }
    }

    private fun createNewClassExpression(expression: PsiNewExpression): Expression {
        val anonymousClass = expression.getAnonymousClass()
        val constructor = expression.resolveMethod()
        val classReference = expression.getClassOrAnonymousClassReference()
        val isNotConvertedClass = classReference != null && !converter.getClassIdentifiers().contains(classReference.getQualifiedName())
        val argumentList = expression.getArgumentList()
        var arguments = argumentList?.getExpressions() ?: array()
        if (constructor == null || constructor.isPrimaryConstructor() || isNotConvertedClass) {
            return NewClassExpression(converter.convertElement(classReference),
                                      converter.convertArguments(expression),
                                      converter.convertExpression(expression.getQualifier()),
                                      if (anonymousClass != null) converter.convertAnonymousClass(anonymousClass) else null)
        }

        val reference = expression.getClassReference()
        val typeParameters = if (reference != null) converter.convertTypes(reference.getTypeParameters()) else listOf()
        return CallChainExpression(Identifier(constructor.getName(), false),
                                   MethodCallExpression(Identifier("init"), converter.convertExpressions(arguments), typeParameters, false))
    }

    private fun createNewEmptyArrayWithoutInitialization(expression: PsiNewExpression): Expression {
        return ArrayWithoutInitializationExpression(
                converter.convertType(expression.getType(), true),
                converter.convertExpressions(expression.getArrayDimensions()))
    }

    private fun createNewEmptyArray(expression: PsiNewExpression): Expression {
        return converter.convertExpression(expression.getArrayInitializer())
    }

    override fun visitParenthesizedExpression(expression: PsiParenthesizedExpression) {
        result = ParenthesizedExpression(converter.convertExpression(expression.getExpression()))
    }

    override fun visitPostfixExpression(expression: PsiPostfixExpression) {
        result = PostfixOperator(getOperatorString(expression.getOperationSign().getTokenType()),
                                   converter.convertExpression(expression.getOperand()))
    }

    override fun visitPrefixExpression(expression: PsiPrefixExpression) {
        val operand = converter.convertExpression(expression.getOperand(), expression.getOperand()!!.getType())
        val token = expression.getOperationTokenType()
        if (token == JavaTokenType.TILDE) {
            result = MethodCallExpression.build(ParenthesizedExpression(operand), "inv", ArrayList())
        }
        else {
            result = PrefixOperator(getOperatorString(token), operand)
        }
    }

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        val containingConstructor = expression.getContainingConstructor()
        val insideSecondaryConstructor = containingConstructor != null && !containingConstructor.isPrimaryConstructor()
        val addReceiver = insideSecondaryConstructor && (expression.getReference()?.resolve() as? PsiField)?.getContainingClass() == containingConstructor!!.getContainingClass()

        val isNullable = converter.convertType(expression.getType(), expression.isResolvedToNotNull()).isNullable
        val referencedName = expression.getReferenceName()!!
        var identifier: Expression = Identifier(referencedName, isNullable)
        val qualifier = expression.getQualifierExpression()

        if (addReceiver) {
            identifier = CallChainExpression(Identifier("__", false), Identifier(referencedName, isNullable))
        }
        else if (insideSecondaryConstructor && expression.isThisConstructorCall()) {
            identifier = Identifier("val __ = " + (containingConstructor?.getContainingClass()?.getNameIdentifier()?.getText() ?: ""))
        }
        else if (qualifier != null && qualifier.getType() is PsiArrayType && referencedName == "length") {
            identifier = Identifier("size", isNullable)
        }
        else if (qualifier == null) {
            val target = expression.getReference()?.resolve()

            if (target is PsiClass) {
                if (PrimitiveType.values() any { it.getTypeName().asString() == target.getName() }) {
                    result = Identifier(target.getQualifiedName()!!, false)
                    return
                }
            }

            if (target is PsiMember
                    && target.hasModifierProperty(PsiModifier.STATIC)
                    && target.getContainingClass() != null
                    && PsiTreeUtil.getParentOfType(expression, javaClass<PsiClass>()) != target.getContainingClass()
                    && !isStaticallyImported(target, expression)) {
                var member = target as PsiMember
                var code = Identifier(referencedName).toKotlin()
                while (member.getContainingClass() != null) {
                    code = Identifier(member.getContainingClass()!!.getName()!!).toKotlin() + "." + code
                    member = member.getContainingClass()!!
                }
                result = Identifier(code, false, false)
                return
            }

            if (target is PsiVariable) {
                val replacement = usageReplacementMap[target]
                if (replacement != null) {
                    identifier = Identifier(replacement, isNullable)
                }
            }
        }

        result = CallChainExpression(converter.convertExpression(qualifier), identifier)
    }

    private fun PsiReference.isResolvedToNotNull(): Boolean {
        val target = resolve()
        return when(target) {
            is PsiEnumConstant -> true
            is PsiModifierListOwner -> target.isAnnotatedAsNotNull()
            else -> false
        }
    }

    override fun visitSuperExpression(expression: PsiSuperExpression) {
        val qualifier = expression.getQualifier()
        result = SuperExpression(if (qualifier != null) Identifier(qualifier.getQualifiedName()!!) else Identifier.Empty)
    }

    override fun visitThisExpression(expression: PsiThisExpression) {
        val qualifier = expression.getQualifier()
        result = ThisExpression(if (qualifier != null) Identifier(qualifier.getQualifiedName()!!) else Identifier.Empty)
    }

    override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
        val castType = expression.getCastType() ?: return
        val operand = expression.getOperand()
        val operandType = operand?.getType()
        val typeText = castType.getType().getCanonicalText()
        val typeConversion = PRIMITIVE_TYPE_CONVERSIONS[typeText]
        if (operandType is PsiPrimitiveType && typeConversion != null) {
            result = MethodCallExpression.build(converter.convertExpression(operand), typeConversion)
        }
        else {
            result = TypeCastExpression(converter.convertType(castType.getType()),
                                        converter.convertExpression(operand))
        }
    }

    override fun visitPolyadicExpression(expression: PsiPolyadicExpression) {
        var parameters = ArrayList<Expression>()
        for (operand : PsiExpression in expression.getOperands()) {
            parameters.add(converter.convertExpression(operand, expression.getType()))
        }
        result = PolyadicExpression(parameters, getOperatorString(expression.getOperationTokenType()))
    }

    private fun getOperatorString(tokenType: IElementType): String {
        return when(tokenType) {
            JavaTokenType.EQEQ -> "=="
            JavaTokenType.NE -> "!="
            JavaTokenType.ANDAND -> "&&"
            JavaTokenType.OROR -> "||"
            JavaTokenType.GT -> ">"
            JavaTokenType.LT -> "<"
            JavaTokenType.GE -> ">="
            JavaTokenType.LE -> "<="
            JavaTokenType.EXCL -> "!"
            JavaTokenType.PLUS -> "+"
            JavaTokenType.MINUS -> "-"
            JavaTokenType.ASTERISK -> "*"
            JavaTokenType.DIV -> "/"
            JavaTokenType.PERC -> "%"
            JavaTokenType.GTGT -> "shr"
            JavaTokenType.LTLT -> "shl"
            JavaTokenType.XOR -> "xor"
            JavaTokenType.AND -> "and"
            JavaTokenType.OR -> "or"
            JavaTokenType.GTGTGT -> "ushr"
            JavaTokenType.PLUSPLUS -> "++"
            JavaTokenType.MINUSMINUS -> "--"
            else -> "" //System.out.println("UNSUPPORTED TOKEN TYPE: " + tokenType?.toString())
        }
    }

    protected fun getClassName(expression: PsiExpression): String {
        var context = expression.getContext()
        while (context != null) {
            val _context = context!!
            if (_context is PsiClass) {
                val identifier = _context.getNameIdentifier()
                if (identifier != null) {
                    return identifier.getText()!!
                }

            }

            context = _context.getContext()
        }
        return ""
    }

    private fun isStaticallyImported(member: PsiMember, context: PsiElement): Boolean {
        val containingFile = context.getContainingFile()
        val targetContainingClass = member.getContainingClass()
        if (containingFile is PsiJavaFile && targetContainingClass != null) {
            val importList = containingFile.getImportList();
            if (importList != null) {
                return importList.getImportStaticStatements().any { importResolvesTo(it, member) }
            }
        }
        return false
    }

    private fun importResolvesTo(importStatement: PsiImportStaticStatement, member: PsiMember): Boolean {
        val targetContainingClass = member.getContainingClass()
        val importedClass = importStatement.resolveTargetClass()
        return importedClass == targetContainingClass
                && (importStatement.isOnDemand() || importStatement.getReferenceName() == member.getName())
    }
}
