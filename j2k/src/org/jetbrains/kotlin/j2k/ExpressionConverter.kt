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

package org.jetbrains.kotlin.j2k


import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Function
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import java.lang.AssertionError

interface ExpressionConverter {
    fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression
}

interface SpecialExpressionConverter {
    fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression?
}

fun ExpressionConverter.withSpecialConverter(specialConverter: SpecialExpressionConverter): ExpressionConverter {
    return object: ExpressionConverter {
        override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter)
                = specialConverter.convertExpression(expression, codeConverter) ?: this@withSpecialConverter.convertExpression(expression, codeConverter)
    }
}

class DefaultExpressionConverter : JavaElementVisitor(), ExpressionConverter {
    private var _codeConverter: CodeConverter? = null
    private var result: Expression = Expression.Empty

    private val codeConverter: CodeConverter get() = _codeConverter!!
    private val typeConverter: TypeConverter get() = codeConverter.typeConverter
    private val converter: Converter get() = codeConverter.converter

    override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression {
        this._codeConverter = codeConverter
        result = Expression.Empty

        expression.accept(this)
        return result
    }

    override fun visitArrayAccessExpression(expression: PsiArrayAccessExpression) {
        val assignment = expression.getStrictParentOfType<PsiAssignmentExpression>()
        val lvalue = assignment != null && expression == assignment.lExpression;
        result = ArrayAccessExpression(codeConverter.convertExpression(expression.arrayExpression),
                                       codeConverter.convertExpression(expression.indexExpression),
                                       lvalue)
    }

    override fun visitArrayInitializerExpression(expression: PsiArrayInitializerExpression) {
        val arrayType = expression.type
        val componentType = (arrayType as? PsiArrayType)?.componentType
        val expressionType = typeConverter.convertType(arrayType)
        assert(expressionType is ArrayType) { "Array initializer must have array type: expressionType = $expressionType expression = $expression" }
        result = createArrayInitializerExpression(expressionType as ArrayType,
                                                  expression.initializers.map { codeConverter.convertExpression(it, componentType) })
    }

    override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
        val tokenType = expression.operationSign.tokenType

        val lhs = codeConverter.convertExpression(expression.lExpression)
        val rhs = codeConverter.convertExpression(expression.rExpression!!, expression.lExpression.type)

        val secondOp = when(tokenType) {
            JavaTokenType.GTGTEQ, JavaTokenType.LTLTEQ, JavaTokenType.GTGTGTEQ,
            JavaTokenType.XOREQ, JavaTokenType.OREQ,
            JavaTokenType.ANDEQ -> true
            else -> false
        }

        val operator = Operator(tokenType).assignPrototype(expression.operationSign)
        if (secondOp) {
            result = AssignmentExpression(lhs, BinaryExpression(lhs, rhs, operator).assignNoPrototype(), Operator.EQ)
        }
        else {
            result = AssignmentExpression(lhs, rhs, operator)
        }
    }

    override fun visitBinaryExpression(expression: PsiBinaryExpression) {
        val left = expression.lOperand
        val right = expression.rOperand

        val operationTokenType = expression.operationTokenType

        val expectedNullability = if (operationTokenType in NON_NULL_OPERAND_OPS) Nullability.NotNull else null

        val leftOperandExpectedType = getOperandExpectedType(left, right, operationTokenType)
        val leftConverted = codeConverter.convertExpression(left, leftOperandExpectedType, expectedNullability)
        val rightConverted = codeConverter.convertExpression(
                right,
                if (leftOperandExpectedType == null)
                    getOperandExpectedType(right, left, operationTokenType)
                else
                    null,
                expectedNullability
        )

        if (operationTokenType == JavaTokenType.GTGTGT) {
            result = MethodCallExpression.buildNonNull(leftConverted, "ushr", ArgumentList.withNoPrototype(rightConverted))
        }
        else {
            var operator = Operator(operationTokenType)
            if (operationTokenType == JavaTokenType.EQEQ || operationTokenType == JavaTokenType.NE) {
                if (!canKeepEqEq(left, right)) {
                    operator = if (operationTokenType == JavaTokenType.EQEQ) Operator(KtTokens.EQEQEQ) else Operator(KtTokens.EXCLEQEQEQ)
                }
            }
            result = BinaryExpression(leftConverted, rightConverted, operator.assignPrototype(expression.operationSign))
        }
    }

    private fun getOperandExpectedType(current: PsiExpression?, other: PsiExpression?, operationTokenType: IElementType): PsiType? {
        val currentType = current?.type
        val otherType = other?.type
        if (currentType !is PsiPrimitiveType || otherType !is PsiPrimitiveType) return null
        if (currentType == PsiType.BOOLEAN || otherType == PsiType.BOOLEAN) return null

        if (operationTokenType == JavaTokenType.EQEQ
            || operationTokenType == JavaTokenType.NE
            || currentType == PsiType.CHAR) {
            if (currentType < otherType) return otherType
        }

        return null
    }

    infix operator fun PsiPrimitiveType.compareTo(other: PsiPrimitiveType): Int {
        return when(this) {
            other -> 0
            PsiType.BYTE -> when(other) {
                PsiType.CHAR -> 1
                else -> -1
            }
            PsiType.SHORT -> when(other) {
                PsiType.CHAR,
                PsiType.BYTE -> 1
                else -> -1
            }
            PsiType.INT -> when(other) {
                PsiType.BYTE,
                PsiType.SHORT,
                PsiType.CHAR -> 1
                else -> -1
            }
            PsiType.LONG -> when(other) {
                PsiType.DOUBLE,
                PsiType.FLOAT -> -1
                else -> 1
            }
            PsiType.FLOAT -> when(other) {
                PsiType.DOUBLE -> -1
                else -> 1
            }
            PsiType.DOUBLE -> 1
            PsiType.CHAR -> -1
            else -> throw AssertionError("Unknown primitive type $this")
        }
    }

    private fun canKeepEqEq(left: PsiExpression, right: PsiExpression?): Boolean {
        if (left.isNullLiteral() || (right?.isNullLiteral() ?: false)) return true
        val type = left.type
        when (type) {
            is PsiPrimitiveType, is PsiArrayType -> return true

            is PsiClassType -> {
                val psiClass = type.resolve() ?: return false
                if (!psiClass.hasModifierProperty(PsiModifier.FINAL)) return false
                if (psiClass.isEnum) return true

                val equalsSignature = getEqualsSignature(converter.project, GlobalSearchScope.allScope(converter.project))
                val equalsMethod = MethodSignatureUtil.findMethodBySignature(psiClass, equalsSignature, true)
                if (equalsMethod != null && equalsMethod.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT) return false

                return true
            }

            else -> return false
        }

    }

    private fun getEqualsSignature(project: Project, scope: GlobalSearchScope): MethodSignature {
        val javaLangObject = PsiType.getJavaLangObject(PsiManager.getInstance(project), scope)
        return MethodSignatureUtil.createMethodSignature("equals", arrayOf<PsiType>(javaLangObject), PsiTypeParameter.EMPTY_ARRAY, PsiSubstitutor.EMPTY)
    }

    private val NON_NULL_OPERAND_OPS = setOf(
            JavaTokenType.ANDAND,
            JavaTokenType.OROR,
            JavaTokenType.PLUS,
            JavaTokenType.MINUS,
            JavaTokenType.ASTERISK,
            JavaTokenType.DIV,
            JavaTokenType.PERC,
            JavaTokenType.LTLT,
            JavaTokenType.GTGT,
            JavaTokenType.GTGTGT)

    override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
        val operand = expression.operand
        val typeName = operand.type.canonicalText
        val primitiveType = JvmPrimitiveType.values().firstOrNull { it.javaKeywordName == typeName }
        val wrapperTypeName = if (primitiveType != null) {
            primitiveType.wrapperFqName
        }
        else if (typeName == "void") { // by unknown reason it's not in JvmPrimitiveType enum
            FqName("java.lang.Void")
        }
        else {
            val type = converter.convertTypeElement(operand, Nullability.NotNull)
            result = QualifiedExpression(ClassLiteralExpression(type).assignNoPrototype(), Identifier.withNoPrototype("java"), null)
            return
        }

        //TODO: need more correct way to detect if short name is ok
        val qualifiedName = wrapperTypeName.asString()
        val classNameToUse = if (qualifiedName in needQualifierNameSet)
            qualifiedName
        else
            wrapperTypeName.shortName().asString()
        result = QualifiedExpression(Identifier(classNameToUse, false).assignPrototype(operand),
                                     Identifier.withNoPrototype("TYPE", isNullable = false),
                                     null)
    }

    override fun visitConditionalExpression(expression: PsiConditionalExpression) {
        val condition = expression.condition
        val type = condition.type
        val expr = if (type != null)
            codeConverter.convertExpression(condition, type)
        else
            codeConverter.convertExpression(condition)
        result = IfStatement(expr,
                             codeConverter.convertExpression(expression.thenExpression),
                             codeConverter.convertExpression(expression.elseExpression),
                             expression.isInSingleLine())
    }

    override fun visitInstanceOfExpression(expression: PsiInstanceOfExpression) {
        val checkType = expression.checkType
        result = IsOperator(codeConverter.convertExpression(expression.operand),
                            converter.convertTypeElement(checkType, Nullability.NotNull))
    }

    override fun visitLiteralExpression(expression: PsiLiteralExpression) {
        val value = expression.value
        var text = expression.text!!
        val type = expression.type
        if (type != null) {
            val typeStr = type.canonicalText
            if (typeStr == "double") {
                text = text.replace("D", "").replace("d", "")
                if (!text.contains(".")) {
                    text += ".0"
                }

            }

            if (typeStr == "float") {
                text = text.replace("F", "f")
            }

            if (typeStr == "long") {
                text = text.replace("l", "L")
            }

            fun isHexLiteral(text: String) = text.startsWith("0x") || text.startsWith("0X")
            fun isLongField(element: PsiElement): Boolean {
                val fieldType = (element as? PsiVariable)?.type ?: return false
                return when (fieldType) {
                    is PsiPrimitiveType -> fieldType.canonicalText == "long"
                    else -> PsiPrimitiveType.getUnboxedType(fieldType)?.canonicalText == "long"
                }
            }

            if (typeStr == "int") {
                val toIntIsNeeded = value != null && value.toString().toInt() < 0 && !isLongField(expression.parent)
                text = if (value != null && !isHexLiteral(text)) value.toString() else text + (if (toIntIsNeeded) ".toInt()" else "")
            }

            if (typeStr == "java.lang.String") {
                text = text.replace("\\$([A-Za-z]+|\\{)".toRegex(), "\\\\$0")
            }
        }

        result = LiteralExpression(text)
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        val methodExpr = expression.methodExpression
        val arguments = expression.argumentList.expressions
        val qualifier = methodExpr.qualifierExpression
        val target = methodExpr.resolve()
        val isNullable = if (target is PsiMethod) typeConverter.methodNullability(target).isNullable(codeConverter.settings) else false
        val typeArguments = convertTypeArguments(expression)

        if (target is KtLightMethod) {
            val origin = target.kotlinOrigin
            val isTopLevel = origin?.getStrictParentOfType<KtClassOrObject>() == null
            if (origin is KtProperty || origin is KtPropertyAccessor || origin is KtParameter) {
                val property = if (origin is KtPropertyAccessor)
                    origin.parent as KtProperty
                else
                    origin as KtNamedDeclaration
                val parameterCount = target.parameterList.parameters.size
                if (parameterCount == arguments.size) {
                    val propertyName = Identifier.withNoPrototype(property.name!!, isNullable)
                    val isExtension = property.isExtensionDeclaration()
                    val propertyAccess = if (isTopLevel) {
                        if (isExtension)
                            QualifiedExpression(codeConverter.convertExpression(arguments.firstOrNull(), true), propertyName, null).assignNoPrototype()
                        else
                            propertyName
                    }
                    else if (qualifier != null) {
                        QualifiedExpression(codeConverter.convertExpression(qualifier), propertyName, methodExpr.dot()).assignNoPrototype()
                    }
                    else {
                        propertyName
                    }

                    when(if (isExtension) parameterCount - 1 else parameterCount) {
                        0 /* getter */ -> {
                            result = propertyAccess
                            return
                        }

                        1 /* setter */ -> {
                            val argument = codeConverter.convertExpression(arguments[if (isExtension) 1 else 0])
                            result = AssignmentExpression(propertyAccess, argument, Operator.EQ)
                            return
                        }
                    }
                }
            }
            else if (origin is KtFunction) {
                if (isTopLevel) {
                    result = if (origin.isExtensionDeclaration()) {
                        val qualifier = codeConverter.convertExpression(arguments.firstOrNull(), true)
                        MethodCallExpression.build(qualifier,
                                                   origin.name!!,
                                                   convertArguments(expression, isExtension = true),
                                                   typeArguments,
                                                   isNullable)
                    }
                    else {
                        MethodCallExpression.build(null,
                                                   origin.name!!,
                                                   convertArguments(expression),
                                                   typeArguments,
                                                   isNullable)
                    }
                    return
                }
            }
            else if (origin == null){
                val resolvedQualifier = (methodExpr.qualifier as? PsiReferenceExpression)?.resolve()
                if (isFacadeClassFromLibrary(resolvedQualifier)) {
                    result = if (target.isKotlinExtensionFunction()) {
                        val qualifier = codeConverter.convertExpression(arguments.firstOrNull(), true)
                        MethodCallExpression.build(qualifier,
                                                   methodExpr.referenceName!!,
                                                   convertArguments(expression, isExtension = true),
                                                   typeArguments,
                                                   isNullable)
                    }
                    else {
                        MethodCallExpression.build(null,
                                                   methodExpr.referenceName!!,
                                                   convertArguments(expression),
                                                   typeArguments,
                                                   isNullable)
                    }
                    return
                }
            }
        }

        if (target is PsiMethod) {
            val specialMethod = SpecialMethod.match(target, arguments.size, converter.services)
            if (specialMethod != null) {
                val data = SpecialMethod.ConvertCallData(qualifier, arguments, typeArguments, codeConverter)
                val converted = specialMethod.convertCall(data)
                if (converted != null) {
                    result = converted
                    return
                }
            }
        }

        /* We move prototypes from methodExpression to full expression because
         * it could contains comments inside, and then we paste them at the end of expression
         * In the case of call, we should paste them after parenthesis
         * ex. Foo./*comment*/bar() -> Foo.bar() /*comment*/
         */
        val methodExpression = codeConverter.convertExpression(methodExpr)
        result = MethodCallExpression(methodExpression,
                                      convertArguments(expression),
                                      typeArguments,
                                      isNullable).assignPrototypesFrom(methodExpression)
        methodExpression.assignNoPrototype()
    }

    private fun KtLightMethod.isKotlinExtensionFunction(): Boolean {
        val origin = this.kotlinOrigin
        if (origin != null) return origin.isExtensionDeclaration()

        val parameters = parameterList.parameters
        return parameters.size > 0 && parameters[0].name == "\$receiver"
    }

    private fun convertTypeArguments(call: PsiCallExpression): List<Type> {
        var typeArgs = call.typeArguments.toList()

        // always add explicit type arguments and remove them if they are redundant later
        if (typeArgs.size == 0) {
            val resolve = call.resolveMethodGenerics()
            if (resolve.isValidResult) {
                val method = resolve.element as? PsiMethod
                if (method != null) {
                    val typeParameters = method.typeParameters
                    if (typeParameters.isNotEmpty()) {
                        val map = resolve.substitutor.substitutionMap
                        typeArgs = typeParameters.map { map[it] ?: return listOf() }
                    }
                }
            }
        }

        return typeArgs.map { typeConverter.convertType(it) }
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        val type = expression.type
        if (expression.arrayInitializer != null) {
            result = codeConverter.convertExpression(expression.arrayInitializer)
        }
        else if (expression.arrayDimensions.size > 0 && expression.type is PsiArrayType) {
            result = ArrayWithoutInitializationExpression(
                    typeConverter.convertType(expression.type, Nullability.NotNull) as ArrayType,
                    codeConverter.convertExpressions(expression.arrayDimensions))
        }
        else {
            if (type?.canonicalText in PsiPrimitiveType.getAllBoxedTypeNames()) {
                val argument = expression.argumentList?.expressions?.singleOrNull()
                if (argument != null && argument.type is PsiPrimitiveType) {
                    result = codeConverter.convertExpression(argument)
                    return
                }
            }

            val qualifier = expression.qualifier
            val classRef = expression.classOrAnonymousClassReference
            val classRefConverted = if (classRef != null) converter.convertCodeReferenceElement(classRef, hasExternalQualifier = qualifier != null) else null

            val anonymousClass = expression.anonymousClass?.let { converter.convertAnonymousClassBody(it) }
            if (isFunctionType(expression.type)) {
                val function = anonymousClass?.body?.members?.singleOrNull() as? Function
                if (function != null) {
                    result = AnonymousFunction(function.returnType.toNotNullType(), function.typeParameterList, function.parameterList, function.body)
                    return
                }
            }
            result = NewClassExpression(classRefConverted,
                                        convertArguments(expression),
                                        codeConverter.convertExpression(qualifier),
                                        anonymousClass)
        }
    }

    override fun visitParenthesizedExpression(expression: PsiParenthesizedExpression) {
        result = ParenthesizedExpression(codeConverter.convertExpression(expression.expression))
    }

    override fun visitPostfixExpression(expression: PsiPostfixExpression) {
        result = PostfixExpression(Operator(expression.operationSign.tokenType).assignPrototype(expression.operationSign),
                                   codeConverter.convertExpression(expression.operand))
    }

    override fun visitPrefixExpression(expression: PsiPrefixExpression) {
        val operand = codeConverter.convertExpression(expression.operand, expression.operand!!.type)
        val token = expression.operationTokenType
        if (token == JavaTokenType.TILDE) {
            result = MethodCallExpression.buildNonNull(operand, "inv")
        }
        else if (token == JavaTokenType.EXCL && operand is BinaryExpression && operand.op.asString() == "==") { // happens when equals is converted to ==
            result = BinaryExpression(operand.left, operand.right, Operator(JavaTokenType.NE).assignPrototype(expression.operand))
        }
        else {
            result = PrefixExpression(Operator(token).assignPrototype(expression.operand), operand)
        }
    }

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        // to avoid quoting of 'this' and 'super' in calls to this/super class constructors
        if (expression.text == "this") {
            result = ThisExpression(Identifier.Empty)
            return
        }
        if (expression.text == "super") {
            result = SuperExpression(Identifier.Empty)
            return
        }

        val referenceName = expression.referenceName!!
        val target = expression.resolve()

        val isNullable = target is PsiVariable && isNullable(target)

        val qualifier = expression.qualifierExpression

        var identifier = Identifier(referenceName, isNullable).assignNoPrototype()


        expression.getContainingClass()?.getParentOfType<PsiVariable>(false)?.let {
            if (it == expression.qualifierExpression?.reference?.resolve()) {
                result = QualifiedExpression(ThisExpression(Identifier.Empty).assignNoPrototype(), identifier, null)
                return
            }
        }

        if (qualifier != null && qualifier.type is PsiArrayType && referenceName == "length") {
            identifier = Identifier.withNoPrototype("size", isNullable)
        }
        else if (qualifier != null) {
            if (target is KtLightField && target.kotlinOrigin is KtObjectDeclaration) {
                result = codeConverter.convertExpression(qualifier)
                return
            }
        }
        else {
            if (target is PsiClass) {
                if (PrimitiveType.values().any { it.typeName.asString() == target.name }) {
                    result = Identifier.withNoPrototype(target.qualifiedName!!, isNullable = false)
                    return
                }
            }

            // add qualification for static members from base classes and also this works for enum constants in switch
            val context = converter.specialContext ?: expression
            if (target is PsiMember
                    && target.hasModifierProperty(PsiModifier.STATIC)
                    && target.containingClass != null
                    && !PsiTreeUtil.isAncestor(target.containingClass, context, true)
                    && !target.isImported(context.containingFile as PsiJavaFile)) {
                var member: PsiMember = target
                var code = Identifier.toKotlin(referenceName)
                while (true) {
                    val containingClass = member.containingClass ?: break
                    code = Identifier.toKotlin(containingClass.name!!) + "." + code
                    member = containingClass
                }
                result = Identifier.withNoPrototype(code, isNullable = false, quotingNeeded = false)
                return
            }
        }

        result = if (qualifier != null)
            QualifiedExpression(codeConverter.convertExpression(qualifier), identifier, expression.dot())
        else
            identifier
    }

    private fun isNullable(target: PsiVariable): Boolean {
        if (typeConverter.variableNullability(target).isNullable(codeConverter.settings)) return true

        if (!converter.inConversionScope(target)) return false

        val canChangeType: Boolean
        when (target) {
            is PsiLocalVariable -> {
                if (converter.settings.specifyLocalVariableTypeByDefault) return false
                canChangeType = codeConverter.canChangeType(target)
            }

            is PsiField -> {
                if (converter.settings.specifyFieldTypeByDefault) return false
                canChangeType = target.hasModifierProperty(PsiModifier.PRIVATE)
            }

            else -> return false
        }

        if (converter.shouldDeclareVariableType(target, converter.typeConverter.convertVariableType(target), canChangeType)) return false

        // if variable type won't be specified then check nullability of the initializer
        return codeConverter.convertExpression(target.initializer).isNullable
    }

    override fun visitSuperExpression(expression: PsiSuperExpression) {
        val psiQualifier = expression.qualifier
        val qualifier = psiQualifier?.referenceName
        result = SuperExpression(if (qualifier != null) Identifier(qualifier).assignPrototype(psiQualifier) else Identifier.Empty)
    }

    override fun visitThisExpression(expression: PsiThisExpression) {
        val psiQualifier = expression.qualifier
        val qualifier = psiQualifier?.referenceName
        result = ThisExpression(if (qualifier != null) Identifier(qualifier).assignPrototype(psiQualifier) else Identifier.Empty)
    }

    override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
        val castType = expression.castType ?: return
        val operand = expression.operand
        val operandType = operand?.type
        val typeText = castType.type.canonicalText
        val typeConversion = PRIMITIVE_TYPE_CONVERSIONS[typeText]
        val operandConverted = codeConverter.convertExpression(operand)
        if (operandType is PsiPrimitiveType && typeConversion != null) {
            result = MethodCallExpression.buildNonNull(operandConverted, typeConversion)
        }
        else {
            val nullability = if (operandConverted.isNullable && !expression.isQualifier())
                Nullability.Nullable
            else
                Nullability.NotNull
            val typeConverted = typeConverter.convertType(castType.type, nullability)
            result = TypeCastExpression(typeConverted, operandConverted)
        }
    }

    private fun PsiExpression.isQualifier(): Boolean {
        val parent = parent
        when (parent) {
            is PsiParenthesizedExpression -> return parent.isQualifier()
            is PsiReferenceExpression -> return this == parent.qualifierExpression
            else -> return false
        }
    }

    override fun visitPolyadicExpression(expression: PsiPolyadicExpression) {
        val args = expression.operands.map {
            codeConverter.convertExpression(it, expression.type).assignPrototype(it, CommentsAndSpacesInheritance.LINE_BREAKS)
        }
        val operators = expression.operands.mapNotNull {
            expression.getTokenBeforeOperand(it)?.let {
                val operator = Operator(it.tokenType)
                val commentsAndSpacesInheritance = if (operator.acceptLineBreakBefore()) CommentsAndSpacesInheritance.LINE_BREAKS else CommentsAndSpacesInheritance.NO_SPACES
                operator.assignPrototype(it, commentsAndSpacesInheritance)
            }
        }

        result = PolyadicExpression(args, operators).assignPrototype(expression)
    }

    private fun convertArguments(expression: PsiCallExpression, isExtension: Boolean = false): ArgumentList {
        val argumentList = expression.argumentList
        var arguments = argumentList?.expressions?.toList() ?: listOf()
        if (isExtension && arguments.isNotEmpty()) {
            arguments = arguments.drop(1)
        }

        val resolved = expression.resolveMethod()
        val parameters = resolved?.parameterList?.parameters ?: arrayOf()

        val commentsAndSpacesInheritance = CommentsAndSpacesInheritance.LINE_BREAKS

        val convertedArguments = if (arguments.size == parameters.size) {
            arguments.mapIndexed { i, argument ->
                val expectedNullability = typeConverter.variableNullability(parameters[i])
                val converted = codeConverter.convertExpression(argument, parameters[i].type, expectedNullability)

                val result = if (i == arguments.lastIndex && parameters[i].isVarArgs && argument.type is PsiArrayType)
                    StarExpression(converted)
                else
                    converted
                result.assignPrototype(argument, commentsAndSpacesInheritance)
            }
        }
        else {
            arguments.map { codeConverter.convertExpression(it).assignPrototype(it, commentsAndSpacesInheritance) }
        }

        val lPar = argumentList?.node?.findChildByType(JavaTokenType.LPARENTH)?.psi
        val rPar = argumentList?.node?.findChildByType(JavaTokenType.RPARENTH)?.psi

        return ArgumentList(
                convertedArguments,
                LPar().assignPrototype(lPar, CommentsAndSpacesInheritance.LINE_BREAKS),
                RPar().assignPrototype(rPar, CommentsAndSpacesInheritance.LINE_BREAKS)
        ).assignPrototype(argumentList)
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        val parameters = expression.parameterList
        val convertedParameters = ParameterList(parameters.parameters.map {
            val paramName = Identifier.withNoPrototype(it.name!!)
            val paramType = if (it.typeElement != null) converter.typeConverter.convertType(it.type) else null
            LambdaParameter(paramName, paramType).assignPrototype(it)
        }, lPar = null, rPar = null).assignPrototype(parameters)

        val body = expression.body
        when (body) {
            is PsiExpression -> {
                val convertedBody = codeConverter.convertExpression(body).assignPrototype(body)
                result = LambdaExpression(convertedParameters, Block.of(convertedBody).assignNoPrototype())
            }
            is PsiCodeBlock -> {
                val convertedBlock = codeConverter.withSpecialStatementConverter(object: SpecialStatementConverter {
                    override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement? {
                        if (statement !is PsiReturnStatement) return null

                        val returnValue = statement.returnValue
                        val methodReturnType = codeConverter.methodReturnType
                        val expressionForReturn = if (returnValue != null && methodReturnType != null)
                            codeConverter.convertExpression(returnValue, methodReturnType)
                        else
                            codeConverter.convertExpression(returnValue)

                        if (body.statements.lastOrNull() == statement) {
                            return expressionForReturn
                        }

                        val callExpression = expression.getParentOfType<PsiMethodCallExpression>(false)
                        if (callExpression != null) {
                            return ReturnStatement(expressionForReturn, Identifier.withNoPrototype(callExpression.methodExpression.text))
                        }

                        return ReturnStatement(expressionForReturn)
                    }

                }).convertBlock(body).assignPrototype(body)
                result = LambdaExpression(convertedParameters, convertedBlock)
            }
        }
    }

    override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
        val qualifierType = PsiMethodReferenceUtil.getQualifierType(expression)
        if (qualifierType is PsiArrayType) {
            result = DummyStringExpression(expression.text + "  /* Currently unsupported in Kotlin */ ")
            return
        }

        val qualifier = expression.qualifier
        if (qualifier == null) {
            // Reference should be qualified
            result = DummyStringExpression(expression.text)
            return
        }

        // todo: For inner classes receiver can be omitted
        val contextClass = expression.getParentOfType<PsiClass>(false)
        val functionalType = expression.functionalInterfaceType

        val isTypeInQualifier = (qualifier as? PsiReference)?.resolve() is PsiClass
        val isKotlinFunctionType = isFunctionType(functionalType)

        // method can be null in case of default constructor
        val method = expression.resolve() as? PsiMethod

        val hasStaticModifier = method?.hasModifierProperty(PsiModifier.STATIC) ?: false
        val needThis = !hasStaticModifier && !expression.isConstructor && isTypeInQualifier

        val parameters = method?.getParametersForMethodReference(needThis, isKotlinFunctionType) ?: emptyList()

        val receiver = when {
            expression.isConstructor -> null
            needThis -> parameters.firstOrNull()
            isTypeInQualifier && method?.containingClass == contextClass -> null
            qualifier is PsiExpression -> codeConverter.convertExpression(qualifier) to null
            else -> null
        }

        val callParams = if (needThis) parameters.drop(1) else parameters

        val specialMethod = method?.let { SpecialMethod.match(it, callParams.size, converter.services) }
        val statement: Statement
        if (expression.isConstructor) {
            val argumentList = ArgumentList.withNoPrototype(callParams.map { it.first })
            statement = MethodCallExpression.buildNonNull(null, convertMethodReferenceQualifier(qualifier), argumentList)
        }
        else if (specialMethod != null) {
            val factory = PsiElementFactory.SERVICE.getInstance(converter.project)
            val fakeReceiver = receiver?.let {
                val psiExpression = if (qualifier is PsiExpression) qualifier else factory.createExpressionFromText("fakeReceiver", null)
                psiExpression.convertedExpression = it.first
                psiExpression
            }
            val fakeParams = callParams.mapIndexed {
                i, param ->
                with(factory.createExpressionFromText("fake$i", null)) {
                    this.convertedExpression = param.first
                    this
                }
            }
            val patchedConverter = codeConverter.withSpecialExpressionConverter(object : SpecialExpressionConverter {
                override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
                    val convertedExpression = expression.convertedExpression
                    expression.convertedExpression = null
                    return convertedExpression
                }
            })

            val callData = SpecialMethod.ConvertCallData(fakeReceiver, fakeParams.toTypedArray(), emptyList(), patchedConverter)
            statement = specialMethod.convertCall(callData)!!
        }
        else {
            val referenceName = expression.referenceName!!
            val argumentList = ArgumentList.withNoPrototype(callParams.map { it.first })
            statement = MethodCallExpression.buildNonNull(receiver?.first, referenceName, argumentList)
        }

        statement.assignNoPrototype()

        val lambdaParameterList = ParameterList(
                if (parameters.size == 1 && !isKotlinFunctionType) {
                    // for lambdas all parameters with types should be present
                    emptyList()
                } else {
                    parameters.map { LambdaParameter(it.first, it.second).assignNoPrototype() }
                },
                lPar = null,
                rPar = null).assignNoPrototype()

        val lambdaExpression = LambdaExpression(lambdaParameterList, Block.of(statement).assignNoPrototype()).assignNoPrototype()

        if (isKotlinFunctionType) {
            result = lambdaExpression
        }
        else {
            val convertedFunctionalType = converter.typeConverter.convertType(functionalType)
            result = MethodCallExpression.buildNonNull(
                    null,
                    convertedFunctionalType.canonicalCode(),
                    ArgumentList.withNoPrototype(lambdaExpression)
            )
        }
    }

    private var PsiExpression.convertedExpression: Expression? by UserDataProperty(Key.create("CONVERTED_EXPRESSION"))

    private fun isFunctionType(functionalType: PsiType?) = functionalType?.canonicalText?.startsWith("kotlin.jvm.functions.Function") ?: false

    private fun convertMethodReferenceQualifier(qualifier: PsiElement): String {
        return when(qualifier) {
            is PsiExpression -> codeConverter.convertExpression(qualifier).canonicalCode()
            is PsiTypeElement -> converter.convertTypeElement(qualifier, Nullability.NotNull).canonicalCode()
            else -> qualifier.text
        }
    }

    private fun PsiMethod.getParametersForMethodReference(needThis: Boolean, isKotlinFunctionType: Boolean): List<Pair<Identifier, Type?>> {
        val newParameters = arrayListOf<Pair<Identifier, Type?>>()

        var thisClassType: ClassType? = null
        val thisClass = containingClass
        if (thisClass != null && isKotlinFunctionType) {
            val containingClassName = thisClass.qualifiedName ?: containingClass!!.name
            if (containingClassName != null) {
                val fqName = FqName(containingClassName)
                val identifier = Identifier.withNoPrototype(fqName.shortName().identifier, imports = listOf(fqName))
                thisClassType = ClassType(
                        ReferenceElement(identifier, converter.convertTypeParameterList(thisClass.typeParameterList).parameters).assignNoPrototype(),
                        Nullability.NotNull,
                        converter.settings).assignNoPrototype()
            }
        }
        if (needThis) newParameters.add(Identifier.withNoPrototype("obj", isNullable = false) to thisClassType)

        parameterList.parameters.forEach {
            val parameterType = if (isKotlinFunctionType) converter.typeConverter.convertType(it.type, Nullability.NotNull) else null
            newParameters.add(Identifier.withNoPrototype(it.name ?: "p", isNullable = false) to parameterType)
        }

        if (newParameters.size == 1 && !isKotlinFunctionType) {
            newParameters.clear()
            newParameters.add(Identifier.withNoPrototype("it", isNullable = false) to null)
        }

        return newParameters
    }

    override fun visitExpression(expression: PsiExpression) {
        result = DummyStringExpression(expression.text)
    }

    companion object {
        private val needQualifierNameSet = setOf("java.lang.Byte", "java.lang.Double", "java.lang.Float", "java.lang.Long", "java.lang.Short")
    }
}
