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
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import com.intellij.psi.impl.light.LightField
import org.jetbrains.jet.lang.resolve.java.JvmAbi

class ExpressionVisitor(private val converter: Converter,
                        private val typeConverter: TypeConverter,
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
        val expressionType = typeConverter.convertType(expression.getType())
        assert(expressionType is ArrayType, "Array initializer must have array type")
        result = createArrayInitializerExpression(expressionType as ArrayType,
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
            result = BinaryExpression(lhs, rhs, getOperatorString(expression.getOperationSign().getTokenType()))
        }
    }

    override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
        val typeElement = converter.convertTypeElement(expression.getOperand())
        result = MethodCallExpression(Identifier("javaClass"), listOf(), listOf(typeElement.`type`.toNotNullType()), false)
    }

    override fun visitConditionalExpression(expression: PsiConditionalExpression) {
        val condition = expression.getCondition()
        val `type` = condition.getType()
        val expr = if (`type` != null)
            converter.convertExpression(condition, `type`)
        else
            converter.convertExpression(condition)
        result = IfStatement(expr,
                             converter.convertExpression(expression.getThenExpression()),
                             converter.convertExpression(expression.getElseExpression()),
                             expression.isInSingleLine())
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
        if (expression.isSuperConstructorCall() && expression.isInsidePrimaryConstructor()) return // skip it

        val methodExpr = expression.getMethodExpression()
        val arguments = expression.getArgumentList().getExpressions()
        val target = methodExpr.resolve()
        val isNullable = if (target is PsiMethod) typeConverter.methodNullability(target).isNullable(converter.settings) else false
        val typeArguments = typeConverter.convertTypes(expression.getTypeArguments())

        if (target is KotlinLightMethod) {
            val origin = target.origin
            val isTopLevel = origin?.getParentByType(javaClass<JetClassOrObject>(), true) == null
            if (origin is JetProperty || origin is JetPropertyAccessor) {
                val property = if (origin is JetProperty) origin else origin.getParent() as JetProperty
                val parameterCount = target.getParameterList().getParameters().size
                if (parameterCount == arguments.size) {
                    val propertyName = Identifier(property.getName()!!, isNullable)
                    val isExtension = property.isExtensionDeclaration()
                    val propertyAccess = if (isTopLevel) {
                        if (isExtension)
                            QualifiedExpression(converter.convertExpression(arguments.firstOrNull()), propertyName)
                        else
                            propertyName
                    }
                    else {
                        QualifiedExpression(converter.convertExpression(methodExpr.getQualifierExpression()), propertyName)
                    }

                    when(if (isExtension) parameterCount - 1 else parameterCount) {
                        0 /* getter */ -> {
                            result = propertyAccess
                            return
                        }

                        1 /* setter */ -> {
                            val argument = converter.convertExpression(arguments[if (isExtension) 1 else 0])
                            result = AssignmentExpression(propertyAccess, argument, "=")
                            return
                        }
                    }
                }
            }
            else if (origin is JetFunction) {
                if (isTopLevel) {
                    result = if (origin.isExtensionDeclaration()) {
                        val qualifier = converter.convertExpression(arguments.firstOrNull())
                        MethodCallExpression(QualifiedExpression(qualifier, Identifier(origin.getName()!!, false)),
                                                      convertArguments(expression, isExtension = true),
                                                      typeArguments,
                                                      isNullable)
                    }
                    else {
                        MethodCallExpression(Identifier(origin.getName()!!, false),
                                                      convertArguments(expression),
                                                      typeArguments,
                                                      isNullable)
                    }
                    return
                }
            }
        }

        if (target is PsiMethod && isObjectEquals(target) && arguments.size == 1) {
            val qualifier = methodExpr.getQualifierExpression()
            if (qualifier != null && qualifier !is PsiSuperExpression) {
                result = BinaryExpression(converter.convertExpression(qualifier), converter.convertExpression(arguments.single()), "==")
                return
            }
        }

        result = MethodCallExpression(converter.convertExpression(methodExpr),
                                      convertArguments(expression),
                                      typeArguments,
                                      isNullable)
    }

    private fun isObjectEquals(method: PsiMethod): Boolean {
        return method.getName() == "equals" &&
                method.getParameterList().getParameters().size == 1 &&
                method.getParameterList().getParameters().single().getType().getCanonicalText() == JAVA_LANG_OBJECT
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        if (expression.getArrayInitializer() != null) {
            result = converter.convertExpression(expression.getArrayInitializer())
        }
        else if (expression.getArrayDimensions().size > 0 && expression.getType() is PsiArrayType) {
            result = ArrayWithoutInitializationExpression(
                    typeConverter.convertType(expression.getType(), Nullability.NotNull) as ArrayType,
                    converter.convertExpressions(expression.getArrayDimensions()))
        }
        else {
            result = createNewClassExpression(expression)
        }
    }

    private fun createNewClassExpression(expression: PsiNewExpression): Expression {
        val anonymousClass = expression.getAnonymousClass()
        val classReference = expression.getClassOrAnonymousClassReference()
        var arguments = expression.getArgumentList()?.getExpressions() ?: array()

        val constructor = expression.resolveMethod()
        if (constructor != null && !constructor.isPrimaryConstructor() && converter.conversionScope.contains(constructor)) {
            //TODO: handle anonymous class!
            // non-primary constructor converted to factory method in class object
            val reference = expression.getClassReference()
            val typeParameters = if (reference != null) typeConverter.convertTypes(reference.getTypeParameters()) else listOf()
            return QualifiedExpression(Identifier(constructor.getName(), false),
                                       MethodCallExpression(Identifier("init"), converter.convertExpressions(arguments), typeParameters, false))
        }

        return NewClassExpression(converter.convertElement(classReference),
                                  convertArguments(expression),
                                  converter.convertExpression(expression.getQualifier()),
                                  if (anonymousClass != null) converter.convertAnonymousClass(anonymousClass) else null)
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
            result = MethodCallExpression.build(operand, "inv", ArrayList())
        }
        else if (token == JavaTokenType.EXCL && operand is BinaryExpression && operand.op == "==") { // happens when equals is converted to ==
            result = BinaryExpression(operand.left, operand.right, "!=")
        }
        else {
            result = PrefixOperator(getOperatorString(token), operand)
        }
    }

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        val target = expression.getReference()?.resolve()
        val isNullable = if (target is PsiVariable) typeConverter.variableNullability(target).isNullable(converter.settings) else false
        val referencedName = expression.getReferenceName()!!
        var identifier: Expression = Identifier(referencedName, isNullable)
        val qualifier = expression.getQualifierExpression()

        val containingConstructor = expression.getContainingConstructor()
        val insideSecondaryConstructor = containingConstructor != null && !containingConstructor.isPrimaryConstructor()

        if (insideSecondaryConstructor && (expression.getReference()?.resolve() as? PsiField)?.getContainingClass() == containingConstructor!!.getContainingClass()) {
            identifier = QualifiedExpression(Identifier("__", false), Identifier(referencedName, isNullable))
        }
        else if (insideSecondaryConstructor && expression.isThisConstructorCall()) {
            identifier = Identifier("val __ = " + (containingConstructor?.getContainingClass()?.getNameIdentifier()?.getText() ?: ""))
        }
        else if (qualifier != null && qualifier.getType() is PsiArrayType && referencedName == "length") {
            identifier = Identifier("size", isNullable)
        }
        else if (qualifier != null) {
            if (referencedName == JvmAbi.CLASS_OBJECT_FIELD || referencedName == JvmAbi.INSTANCE_FIELD) {
                if (target is LightField) { //TODO: should be KotlinLightField with check of origin here, see KT-5188
                    result = converter.convertExpression(qualifier)
                    return
                }
            }
        }
        else {
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
                var member: PsiMember = target
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

        result = QualifiedExpression(converter.convertExpression(qualifier), identifier)
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
            result = TypeCastExpression(typeConverter.convertType(castType.getType()),
                                        converter.convertExpression(operand))
        }
    }

    override fun visitPolyadicExpression(expression: PsiPolyadicExpression) {
        val args = expression.getOperands().map { converter.convertExpression(it, expression.getType()) }
        result = PolyadicExpression(args, getOperatorString(expression.getOperationTokenType()))
    }

    private fun convertArguments(expression: PsiCallExpression, isExtension: Boolean = false): List<Expression> {
        var arguments = expression.getArgumentList()?.getExpressions()?.toList() ?: listOf()
        if (isExtension && arguments.isNotEmpty()) {
            arguments = arguments.drop(1)
        }
        val resolved = expression.resolveMethod()
        val expectedTypes = if (resolved != null)
            resolved.getParameterList().getParameters().map { it.getType() }
        else
            listOf()

        return if (arguments.size == expectedTypes.size())
            (0..expectedTypes.lastIndex).map { i -> converter.convertExpression(arguments[i], expectedTypes[i]) }
        else
            arguments.map { converter.convertExpression(it) }
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
