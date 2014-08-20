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
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType
import org.jetbrains.jet.lang.resolve.name.FqName

open class ExpressionVisitor(private val converter: Converter) : JavaElementVisitor() {
    private val typeConverter = converter.typeConverter

    public var result: Expression = Expression.Empty
        protected set

    public fun reset() {
        result = Expression.Empty
    }

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
                                                  converter.convertExpressions(expression.getInitializers()),
                                                  needExplicitType = true/*TODO: it's often redundant*/)
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
            result = AssignmentExpression(lhs, BinaryExpression(lhs, rhs, secondOp).assignNoPrototype(), " = ")
        }
        else {
            result = AssignmentExpression(lhs, rhs, expression.getOperationSign().getText()!!)
        }
    }

    override fun visitBinaryExpression(expression: PsiBinaryExpression) {
        val operandsExpectedType = when (expression.getOperationTokenType()) {
            JavaTokenType.ANDAND, JavaTokenType.OROR -> PsiType.BOOLEAN

            JavaTokenType.PLUS, JavaTokenType.MINUS, JavaTokenType.ASTERISK,
            JavaTokenType.DIV, JavaTokenType.PERC, JavaTokenType.LTLT, JavaTokenType.GTGT,
            JavaTokenType.GTGTGT -> expression.getType()

            else -> null
        }
        val lhs = converter.convertExpression(expression.getLOperand(), operandsExpectedType)
        val rhs = converter.convertExpression(expression.getROperand(), operandsExpectedType)
        if (expression.getOperationTokenType() == JavaTokenType.GTGTGT) {
            result = MethodCallExpression.buildNotNull(lhs, "ushr", listOf(rhs))
        }
        else {
            result = BinaryExpression(lhs, rhs, getOperatorString(expression.getOperationSign().getTokenType()))
        }
    }

    override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
        val operand = expression.getOperand()
        val typeName = operand.getType().getCanonicalText()
        val primitiveType = JvmPrimitiveType.values().firstOrNull { it.getName() == typeName }
        val wrapperTypeName = if (primitiveType != null) {
            primitiveType.getWrapperFqName()
        }
        else if (typeName == "void") { // by unknown reason it's not in JvmPrimitiveType enum
            FqName("java.lang.Void")
        }
        else {
            val typeElement = converter.convertTypeElement(operand)
            result = MethodCallExpression.buildNotNull(null, "javaClass", listOf(), listOf(typeElement.`type`.toNotNullType()))
            return
        }

        //TODO: need more correct way to detect if short name is ok
        val qualifiedName = wrapperTypeName.asString()
        val classNameToUse = if (qualifiedName in needQualifierNameSet)
            qualifiedName
        else
            wrapperTypeName.shortName().asString()
        result = QualifiedExpression(Identifier(classNameToUse, false).assignPrototype(operand),
                                     Identifier("TYPE", false).assignNoPrototype())
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
        val methodExpr = expression.getMethodExpression()
        val arguments = expression.getArgumentList().getExpressions()
        val target = methodExpr.resolve()
        val isNullable = if (target is PsiMethod) typeConverter.methodNullability(target).isNullable(converter.settings) else false
        val typeArguments = convertTypeArguments(expression)

        if (target is KotlinLightMethod) {
            val origin = target.origin
            val isTopLevel = origin?.getParentByType(javaClass<JetClassOrObject>(), true) == null
            if (origin is JetProperty || origin is JetPropertyAccessor || origin is JetParameter) {
                val property = if (origin is JetPropertyAccessor)
                    origin.getParent() as JetProperty
                else
                    origin as JetNamedDeclaration
                val parameterCount = target.getParameterList().getParameters().size
                if (parameterCount == arguments.size) {
                    val propertyName = Identifier(property.getName()!!, isNullable).assignNoPrototype()
                    val isExtension = property.isExtensionDeclaration()
                    val propertyAccess = if (isTopLevel) {
                        if (isExtension)
                            QualifiedExpression(converter.convertExpression(arguments.firstOrNull()), propertyName).assignNoPrototype()
                        else
                            propertyName
                    }
                    else {
                        QualifiedExpression(converter.convertExpression(methodExpr.getQualifierExpression()), propertyName).assignNoPrototype()
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
                        MethodCallExpression.build(qualifier,
                                                   origin.getName()!!,
                                                   convertArguments(expression, isExtension = true),
                                                   typeArguments,
                                                   isNullable)
                    }
                    else {
                        MethodCallExpression.build(null,
                                                   origin.getName()!!,
                                                   convertArguments(expression),
                                                   typeArguments,
                                                   isNullable)
                    }
                    return
                }
            }
        }

        if (target is PsiMethod) {
            val specialMethod = SpecialMethod.values().firstOrNull { it.matches(target) }
            if (specialMethod != null && arguments.size == specialMethod.parameterCount) {
                val converted = specialMethod.convertCall(methodExpr.getQualifierExpression(), arguments, typeArguments, converter)
                if (converted != null) {
                    result = converted
                    return
                }
            }
        }

        result = MethodCallExpression(converter.convertExpression(methodExpr),
                                      convertArguments(expression),
                                      typeArguments,
                                      isNullable)
    }

    private fun convertTypeArguments(call: PsiCallExpression): List<Type> {
        var typeArgs = call.getTypeArguments().toList()

        // always add explicit type arguments and remove them if they are redundant later
        if (typeArgs.size == 0) {
            val resolve = call.resolveMethodGenerics()
            if (resolve.isValidResult()) {
                val method = resolve.getElement() as? PsiMethod
                if (method != null) {
                    val typeParameters = method.getTypeParameters()
                    if (typeParameters.isNotEmpty()) {
                        val map = resolve.getSubstitutor().getSubstitutionMap()
                        typeArgs = typeParameters.map { map[it] ?: return listOf() }
                    }
                }
            }
        }

        return typeArgs.map { typeConverter.convertType(it).assignNoPrototype() }
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
            val anonymousClass = expression.getAnonymousClass()
            val qualifier = expression.getQualifier()
            val classRef = expression.getClassOrAnonymousClassReference()
            val classRefConverted = if (classRef != null) converter.convertCodeReferenceElement(classRef, hasExternalQualifier = qualifier != null) else null
            result = NewClassExpression(classRefConverted,
                                      convertArguments(expression),
                                      converter.convertExpression(qualifier),
                                      if (anonymousClass != null) converter.convertAnonymousClassBody(anonymousClass) else null)
        }
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
            result = MethodCallExpression.buildNotNull(operand, "inv")
        }
        else if (token == JavaTokenType.EXCL && operand is BinaryExpression && operand.op == "==") { // happens when equals is converted to ==
            result = BinaryExpression(operand.left, operand.right, "!=")
        }
        else {
            result = PrefixOperator(getOperatorString(token), operand)
        }
    }

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        val referenceName = expression.getReferenceName()!!
        val target = expression.getReference()?.resolve()
        val isNullable = target is PsiVariable && typeConverter.variableNullability(target).isNullable(converter.settings)
        val qualifier = expression.getQualifierExpression()

        var identifier = Identifier(referenceName, isNullable).assignNoPrototype()
        if (qualifier != null && qualifier.getType() is PsiArrayType && referenceName == "length") {
            identifier = Identifier("size", isNullable).assignNoPrototype()
        }
        else if (qualifier != null) {
            if (referenceName == JvmAbi.CLASS_OBJECT_FIELD || referenceName == JvmAbi.INSTANCE_FIELD) {
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

            // add qualification for static members from base classes and also this works for enum constants in switch
            val context = converter.specialContext ?: expression
            if (target is PsiMember
                    && target.hasModifierProperty(PsiModifier.STATIC)
                    && target.getContainingClass() != null
                    && !PsiTreeUtil.isAncestor(target.getContainingClass(), context, true)
                    && !target.isImported(context.getContainingFile() as PsiJavaFile)) {
                var member: PsiMember = target
                var code = Identifier.toKotlin(referenceName)
                while (true) {
                    val containingClass = member.getContainingClass() ?: break
                    code = Identifier.toKotlin(containingClass.getName()!!) + "." + code
                    member = containingClass
                }
                result = Identifier(code, false, false)
                return
            }
        }

        result = if (qualifier != null) QualifiedExpression(converter.convertExpression(qualifier), identifier) else identifier
    }

    override fun visitSuperExpression(expression: PsiSuperExpression) {
        val psiQualifier = expression.getQualifier()
        val qualifier = psiQualifier?.getReferenceName()
        result = SuperExpression(if (qualifier != null) Identifier(qualifier).assignPrototype(psiQualifier) else Identifier.Empty)
    }

    override fun visitThisExpression(expression: PsiThisExpression) {
        val psiQualifier = expression.getQualifier()
        val qualifier = psiQualifier?.getReferenceName()
        result = ThisExpression(if (qualifier != null) Identifier(qualifier).assignPrototype(psiQualifier) else Identifier.Empty)
    }

    override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
        val castType = expression.getCastType() ?: return
        val operand = expression.getOperand()
        val operandType = operand?.getType()
        val typeText = castType.getType().getCanonicalText()
        val typeConversion = PRIMITIVE_TYPE_CONVERSIONS[typeText]
        if (operandType is PsiPrimitiveType && typeConversion != null) {
            result = MethodCallExpression.buildNotNull(converter.convertExpression(operand), typeConversion)
        }
        else {
            result = TypeCastExpression(typeConverter.convertType(castType.getType()), converter.convertExpression(operand))
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
        val parameters = resolved?.getParameterList()?.getParameters()
        val expectedTypes = parameters?.map { it.getType() } ?: listOf()

        return if (arguments.size == expectedTypes.size())
            (0..arguments.lastIndex).map { i ->
                val argument = arguments[i]
                val converted = converter.convertExpression(argument, expectedTypes[i])
                if (parameters != null && i == arguments.lastIndex && parameters[i].isVarArgs() && argument.getType() is PsiArrayType)
                    StarExpression(converted).assignNoPrototype()
                else
                    converted
            }
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

    class object {
        private val needQualifierNameSet = setOf("java.lang.Byte", "java.lang.Double", "java.lang.Float", "java.lang.Long", "java.lang.Short")
    }
}
