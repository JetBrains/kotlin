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
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import java.util.ArrayList
import java.util.Collections
import com.intellij.psi.CommonClassNames.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.types.lang.PrimitiveType
import org.jetbrains.jet.j2k.isAnnotatedAsNotNull

public open class ExpressionVisitor(converter: Converter) : StatementVisitor(converter) {
    {
        myResult = Expression.EMPTY_EXPRESSION
    }

    public override fun getResult(): Expression {
        return myResult as Expression
    }

    public override fun visitArrayAccessExpression(expression: PsiArrayAccessExpression?) {
        val assignment = PsiTreeUtil.getParentOfType(expression, javaClass<PsiAssignmentExpression>())
        val lvalue = assignment != null && expression == assignment.getLExpression();
        myResult = ArrayAccessExpression(getConverter().expressionToExpression(expression?.getArrayExpression()),
                                         getConverter().expressionToExpression(expression?.getIndexExpression()),
                                         lvalue)
    }

    public override fun visitArrayInitializerExpression(expression: PsiArrayInitializerExpression?) {
        myResult = ArrayInitializerExpression(getConverter().typeToType(expression?.getType()),
                                              getConverter().expressionsToExpressionList(expression?.getInitializers()!!))
    }

    public override fun visitAssignmentExpression(expression: PsiAssignmentExpression?) {
        val tokenType: IElementType = expression?.getOperationSign()?.getTokenType()!!
        val secondOp: String = when(tokenType) {
            JavaTokenType.GTGTEQ -> "shr"
            JavaTokenType.LTLTEQ -> "shl"
            JavaTokenType.XOREQ -> "xor"
            JavaTokenType.ANDEQ -> "and"
            JavaTokenType.OREQ -> "or"
            JavaTokenType.GTGTGTEQ -> "ushr"
            else -> ""
        }

        val lhs = getConverter().expressionToExpression(expression?.getLExpression()!!)
        val rhs = getConverter().expressionToExpression(expression?.getRExpression()!!, expression?.getLExpression()?.getType())
        if (!secondOp.isEmpty()) {
            myResult = AssignmentExpression(lhs, BinaryExpression(lhs, rhs, secondOp), "=")
        }
        else {
            myResult = AssignmentExpression(lhs, rhs, expression?.getOperationSign()?.getText()!!)
        }
    }

    public override fun visitBinaryExpression(expression: PsiBinaryExpression?) {
        val lhs = getConverter().expressionToExpression(expression?.getLOperand()!!, expression?.getType())
        val rhs = getConverter().expressionToExpression(expression?.getROperand(), expression?.getType())
        if (expression?.getOperationSign()?.getTokenType() == JavaTokenType.GTGTGT) {
            myResult = MethodCallExpression.build(lhs, "ushr", arrayList(rhs))
        }
        else {
            myResult = BinaryExpression(lhs, rhs,
                                        getOperatorString(expression?.getOperationSign()?.getTokenType()!!))
        }
    }

    public override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression?) {
        myResult = ClassObjectAccessExpression(getConverter().typeElementToTypeElement(expression?.getOperand()))
    }

    public override fun visitConditionalExpression(expression: PsiConditionalExpression?) {
        val condition: PsiExpression? = expression?.getCondition()
        val `type`: PsiType? = condition?.getType()
        val e: Expression = (if (`type` != null)
            getConverter().expressionToExpression(condition, `type`)
        else
            getConverter().expressionToExpression(condition))
        myResult = ParenthesizedExpression(IfStatement(e,
                                                       getConverter().expressionToExpression(expression?.getThenExpression()),
                                                       getConverter().expressionToExpression(expression?.getElseExpression())))
    }

    public override fun visitExpressionList(list: PsiExpressionList?) {
        myResult = ExpressionList(getConverter().expressionsToExpressionList(list!!.getExpressions()))
    }

    public override fun visitInstanceOfExpression(expression: PsiInstanceOfExpression?) {
        val checkType: PsiTypeElement? = expression?.getCheckType()
        myResult = IsOperator(getConverter().expressionToExpression(expression?.getOperand()),
                              myConverter.typeElementToTypeElement(checkType))
    }

    public override fun visitLiteralExpression(expression: PsiLiteralExpression?) {
        val value: Any? = expression?.getValue()
        var text: String = expression?.getText()!!
        val `type`: PsiType? = expression?.getType()
        if (`type` != null) {
            val canonicalTypeStr: String? = `type`.getCanonicalText()
            if (canonicalTypeStr?.equals("double")!! || canonicalTypeStr?.equals(JAVA_LANG_DOUBLE)!!) {
                text = text.replace("D", "").replace("d", "")
                if (!text.contains(".")) {
                    text += ".0"
                }

            }

            if (canonicalTypeStr?.equals("float")!! || canonicalTypeStr?.equals(JAVA_LANG_FLOAT)!!) {
                text = text.replace("F", "").replace("f", "") + "." + OperatorConventions.FLOAT + "()"
            }

            if (canonicalTypeStr?.equals("long")!! || canonicalTypeStr?.equals(JAVA_LANG_LONG)!!) {
                text = text.replace("L", "").replace("l", "")
            }

            if (canonicalTypeStr?.equals("int")!! || canonicalTypeStr?.equals(JAVA_LANG_INTEGER)!!) {
                text = (if (value != null) value.toString() else text)
            }
        }

        myResult = LiteralExpression(text)
    }

    public override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
        convertMethodCallExpression(expression!!)
    }

    protected fun convertMethodCallExpression(expression: PsiMethodCallExpression) {
        if (!SuperVisitor.isSuper(expression.getMethodExpression()) || !isInsidePrimaryConstructor(expression)) {
            myResult = MethodCallExpression(getConverter().expressionToExpression(expression.getMethodExpression()),
                                            getConverter().argumentsToExpressionList(expression),
                                            getConverter().typesToTypeList(expression.getTypeArguments()),
                                            getConverter().typeToType(expression.getType()).nullable)
        }
    }

    public override fun visitNewExpression(expression: PsiNewExpression?) {
        if (expression?.getArrayInitializer() != null)
        {
            myResult = createNewEmptyArray(expression)
        }
        else
            if (expression?.getArrayDimensions()?.size!! > 0) {
                myResult = createNewEmptyArrayWithoutInitialization(expression!!)
            }
            else
            {
                myResult = createNewClassExpression(expression)
            }
    }

    private fun createNewClassExpression(expression: PsiNewExpression?): Expression {
        val anonymousClass: PsiAnonymousClass? = expression?.getAnonymousClass()
        val constructor: PsiMethod? = expression?.resolveMethod()
        var classReference: PsiJavaCodeReferenceElement? = expression?.getClassOrAnonymousClassReference()
        val isNotConvertedClass: Boolean = classReference != null && !getConverter().getClassIdentifiers().contains(classReference?.getQualifiedName())
        var argumentList: PsiExpressionList? = expression?.getArgumentList()
        var arguments: Array<PsiExpression> = (if (argumentList != null)
            argumentList?.getExpressions()!!
        else
            array<PsiExpression>())
        if (constructor == null || Converter.isConstructorPrimary(constructor) || isNotConvertedClass)
        {
            return NewClassExpression(getConverter().elementToElement(classReference),
                                      getConverter().argumentsToExpressionList(expression!!),
                                      getConverter().expressionToExpression(expression.getQualifier()),
                                      (if (anonymousClass != null)
                                          getConverter().anonymousClassToAnonymousClass(anonymousClass)
                                      else
                                          null))
        }

        val reference: PsiJavaCodeReferenceElement? = expression?.getClassReference()
        val typeParameters: List<Type> = (if (reference != null)
            getConverter().typesToTypeList(reference.getTypeParameters())
        else
            Collections.emptyList<Type>())
        return CallChainExpression(Identifier(constructor.getName(), false),
                                   MethodCallExpression(Identifier("init"), getConverter().expressionsToExpressionList(arguments), typeParameters, false))
    }

    private fun createNewEmptyArrayWithoutInitialization(expression: PsiNewExpression): Expression {
        return ArrayWithoutInitializationExpression(
                getConverter().typeToType(expression.getType(), true),
                getConverter().expressionsToExpressionList(expression.getArrayDimensions()))
    }

    private fun createNewEmptyArray(expression: PsiNewExpression?): Expression {
        return getConverter().expressionToExpression(expression?.getArrayInitializer())
    }

    public override fun visitParenthesizedExpression(expression: PsiParenthesizedExpression?) {
        myResult = ParenthesizedExpression(getConverter().expressionToExpression(expression?.getExpression()))
    }

    public override fun visitPostfixExpression(expression: PsiPostfixExpression?) {
        myResult = PostfixOperator(getOperatorString(expression!!.getOperationSign().getTokenType()!!),
                                   getConverter().expressionToExpression(expression.getOperand()))
    }

    public override fun visitPrefixExpression(expression: PsiPrefixExpression?) {
        val operand = getConverter().expressionToExpression(expression?.getOperand(), expression?.getOperand()!!.getType())
        val token = expression?.getOperationTokenType()!!
        if (token == JavaTokenType.TILDE) {
            myResult = MethodCallExpression.build(ParenthesizedExpression(operand), "inv", arrayList())
        }
        else {
            myResult = PrefixOperator(getOperatorString(token), operand)
        }
    }

    public override fun visitReferenceExpression(expression: PsiReferenceExpression?) {
        val isFieldReference: Boolean = isFieldReference(expression!!, getContainingClass(expression))
        val insideSecondaryConstructor: Boolean = isInsideSecondaryConstructor(expression)
        val hasReceiver: Boolean = isFieldReference && insideSecondaryConstructor
        val isThis: Boolean = isThisExpression(expression)
        val notNull = isResolvedToNotNull(expression)
        val isNullable: Boolean = getConverter().typeToType(expression.getType(), notNull).nullable
        val className: String = getClassNameWithConstructor(expression)
        val referencedName = expression.getReferenceName()!!
        var identifier: Expression = Identifier(referencedName, isNullable)
        val __: String = "__"
        val qualifier = expression.getQualifierExpression()
        if (hasReceiver){
            identifier = CallChainExpression(Identifier(__, false), Identifier(referencedName, isNullable))
        }
        else if (insideSecondaryConstructor && isThis) {
            identifier = Identifier("val __ = " + className)
        }
        else if (qualifier != null && qualifier.getType() is PsiArrayType && referencedName == "length") {
            identifier = Identifier("size", isNullable)
        }
        else if (qualifier == null) {
            val resolved = expression.getReference()?.resolve()
            if (resolved is PsiClass) {
                val clazz = resolved as PsiClass

                if (PrimitiveType.values() any { it.getTypeName().asString() == clazz.getName() }) {
                    myResult = Identifier(clazz.getQualifiedName()!!, false)
                    return
                }
            }
            if (resolved is PsiMember && resolved.hasModifierProperty(PsiModifier.STATIC) &&
            resolved.getContainingClass() != null &&
            PsiTreeUtil.getParentOfType(expression, javaClass<PsiClass>()) != resolved.getContainingClass() &&
            !isStaticallyImported(resolved, expression)) {
                var member = resolved as PsiMember
                var result = Identifier(referencedName).toKotlin()
                while(member.getContainingClass() != null) {
                    result = Identifier(member.getContainingClass()!!.getName()!!).toKotlin() + "." + result
                    member = member.getContainingClass()!!
                }
                myResult = Identifier(result, false, false)
                return
            }
        }

        myResult = CallChainExpression(getConverter().expressionToExpression(qualifier), identifier)
    }

    private fun isResolvedToNotNull(expression: PsiReference): Boolean {
        val target = expression.resolve()
        if (target is PsiEnumConstant) {
            return true;
        }
        if (target is PsiModifierListOwner) {
            return isAnnotatedAsNotNull(target.getModifierList());
        }
        return false;
    }

    public override fun visitSuperExpression(expression: PsiSuperExpression?) {
        val qualifier: PsiJavaCodeReferenceElement? = expression?.getQualifier()
        myResult = SuperExpression((if (qualifier != null)
            Identifier(qualifier.getQualifiedName()!!)
        else
            Identifier.EMPTY_IDENTIFIER))
    }

    public override fun visitThisExpression(expression: PsiThisExpression?) {
        val qualifier: PsiJavaCodeReferenceElement? = expression?.getQualifier()
        myResult = ThisExpression((if (qualifier != null)
            Identifier(qualifier.getQualifiedName()!!)
        else
            Identifier.EMPTY_IDENTIFIER))
    }

    public override fun visitTypeCastExpression(expression: PsiTypeCastExpression?) {
        val castType: PsiTypeElement? = expression?.getCastType()
        if (castType != null) {
            val operand = expression?.getOperand()
            val operandType = operand?.getType()
            val typeText = castType.getType().getCanonicalText()
            val typeConversion = Converter.PRIMITIVE_TYPE_CONVERSIONS[typeText]
            if (operandType is PsiPrimitiveType && typeConversion != null) {
                myResult = MethodCallExpression.build(getConverter().expressionToExpression(operand), typeConversion)
            }
            else {
                myResult = TypeCastExpression(getConverter().typeToType(castType.getType()),
                                              getConverter().expressionToExpression(operand))
            }
        }
    }

    public override fun visitPolyadicExpression(expression: PsiPolyadicExpression?) {
        var parameters = ArrayList<Expression>()
        for (operand : PsiExpression in expression?.getOperands()!!) {
            parameters.add(getConverter().expressionToExpression(operand, expression?.getType()))
        }
        myResult = PolyadicExpression(parameters, getOperatorString(expression?.getOperationTokenType()!!))
    }

    class object {
        private fun getOperatorString(tokenType: IElementType): String {
            if (tokenType == JavaTokenType.PLUS)
                return "+"

            if (tokenType == JavaTokenType.MINUS)
                return "-"

            if (tokenType == JavaTokenType.ASTERISK)
                return "*"

            if (tokenType == JavaTokenType.DIV)
                return "/"

            if (tokenType == JavaTokenType.PERC)
                return "%"

            if (tokenType == JavaTokenType.GTGT)
                return "shr"

            if (tokenType == JavaTokenType.LTLT)
                return "shl"

            if (tokenType == JavaTokenType.XOR)
                return "xor"

            if (tokenType == JavaTokenType.AND)
                return "and"

            if (tokenType == JavaTokenType.OR)
                return "or"

            if (tokenType == JavaTokenType.GTGTGT)
                return "ushr"

            if (tokenType == JavaTokenType.GT)
                return ">"

            if (tokenType == JavaTokenType.LT)
                return "<"

            if (tokenType == JavaTokenType.GE)
                return ">="

            if (tokenType == JavaTokenType.LE)
                return "<="

            if (tokenType == JavaTokenType.EQEQ)
                return "=="

            if (tokenType == JavaTokenType.NE)
                return "!="

            if (tokenType == JavaTokenType.ANDAND)
                return "&&"

            if (tokenType == JavaTokenType.OROR)
                return "||"

            if (tokenType == JavaTokenType.PLUSPLUS)
                return "++"

            if (tokenType == JavaTokenType.MINUSMINUS)
                return "--"

            if (tokenType == JavaTokenType.EXCL)
                return "!"

            //            System.out.println("UNSUPPORTED TOKEN TYPE: " + tokenType?.toString())
            return ""
        }

        private fun getClassNameWithConstructor(expression: PsiReferenceExpression): String {
            var context: PsiElement? = expression.getContext()
            while (context != null) {
                if (context is PsiMethod && ((context as PsiMethod)).isConstructor()) {
                    val containingClass: PsiClass? = ((context as PsiMethod)).getContainingClass()
                    if (containingClass != null) {
                        val identifier: PsiIdentifier? = containingClass.getNameIdentifier()
                        if (identifier != null) {
                            return identifier.getText()!!
                        }

                    }

                }

                context = context?.getContext()
            }
            return ""
        }

        open fun getClassName(expression: PsiExpression): String {
            var context: PsiElement? = expression.getContext()
            while (context != null)
            {
                if ((context is PsiClass?)) {
                    val containingClass: PsiClass? = (context as PsiClass?)
                    val identifier: PsiIdentifier? = containingClass?.getNameIdentifier()
                    if (identifier != null) {
                        return identifier.getText()!!
                    }

                }

                context = context?.getContext()
            }
            return ""
        }

        private fun isFieldReference(expression: PsiReferenceExpression, currentClass: PsiClass?): Boolean {
            val reference: PsiReference? = expression.getReference()
            if (reference != null) {
                val resolvedReference: PsiElement? = reference.resolve()
                if (resolvedReference is PsiField) {
                    return (resolvedReference as PsiField).getContainingClass() == currentClass
                }
            }

            return false
        }

        private fun isInsideSecondaryConstructor(expression: PsiReferenceExpression): Boolean {
            var context: PsiElement? = expression.getContext()
            while (context != null) {
                if (context is PsiMethod && (context as PsiMethod).isConstructor()) {
                    return !Converter.isConstructorPrimary((context as PsiMethod))
                }

                context = context?.getContext()
            }
            return false
        }

        private fun isInsidePrimaryConstructor(expression: PsiExpression): Boolean {
            var context: PsiElement? = expression.getContext()
            while (context != null) {
                if (context is PsiMethod && (context as PsiMethod).isConstructor()) {
                    return Converter.isConstructorPrimary(context as PsiMethod)
                }

                context = context?.getContext()
            }
            return false
        }

        private fun getContainingClass(expression: PsiExpression): PsiClass? {
            var context: PsiElement? = expression.getContext()
            while (context != null)
            {
                if (context is PsiMethod && (context as PsiMethod).isConstructor())
                {
                    return (context as PsiMethod).getContainingClass()
                }

                context = context?.getContext()
            }
            return null
        }
        private fun isThisExpression(expression: PsiReferenceExpression): Boolean {
            for (r : PsiReference? in expression.getReferences())
                if (r?.getCanonicalText()?.equals("this")!!)
                {
                    val res: PsiElement? = r?.resolve()
                    if (res is PsiMethod && res.isConstructor()) {
                        return true
                    }

                }

            return false
        }
    }
}

private fun isStaticallyImported(member: PsiMember, context: PsiElement): Boolean {
    val containingFile = context.getContainingFile()
    val targetContainingClass = member.getContainingClass()
    if (containingFile is PsiJavaFile && targetContainingClass != null) {
        val importList = containingFile.getImportList();
        if (importList != null) {
            val importStatics = importList.getImportStaticStatements();
            return importStatics.any { importResolvesTo(it, member) }
        }
    }
    return false
}

private fun importResolvesTo(stmt: PsiImportStaticStatement?, member: PsiMember): Boolean {
    val targetContainingClass = member.getContainingClass()
    var importedClass = stmt?.resolveTargetClass()
    return importedClass == targetContainingClass && (stmt?.isOnDemand() ?: false ||
    stmt?.getReferenceName() == member.getName())
}
