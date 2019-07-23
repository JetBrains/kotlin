/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.nj2k.tree.impl

import com.intellij.psi.JavaTokenType
import com.intellij.psi.impl.source.tree.ElementType.OPERATION_BIT_SET
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

class JKJavaFieldImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement,
    mutabilityElement: JKMutabilityModifierElement
) : JKJavaField(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaField(this, data)

    override var annotationList: JKAnnotationList by child(annotationList)
    override var initializer: JKExpression by child(initializer)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
    override var mutabilityElement by child(mutabilityElement)
}

class JKJavaMethodImpl(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    typeParameterList: JKTypeParameterList,
    annotationList: JKAnnotationList,
    throwsList: List<JKTypeElement>,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKJavaMethod(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethod(this, data)

    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var typeParameterList: JKTypeParameterList by child(typeParameterList)
    override var annotationList: JKAnnotationList by child(annotationList)
    override var throwsList: List<JKTypeElement> by children(throwsList)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
}

class JKJavaLiteralExpressionImpl(
    override val literal: String,
    override val type: JKLiteralExpression.LiteralType
) : JKJavaLiteralExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaLiteralExpression(this, data)

    init {
        require(type in setOf(STRING, CHAR, INT, LONG, FLOAT, DOUBLE))
    }
}


class JKJavaOperatorToken(val psiToken: IElementType) : JKOperatorToken {
    override val text: String
        get() = when (psiToken) {
            JavaTokenType.EQ -> "="
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
            JavaTokenType.PLUSEQ -> "+="
            JavaTokenType.MINUSEQ -> "-="
            JavaTokenType.ASTERISKEQ -> "*="
            JavaTokenType.DIVEQ -> "/="
            JavaTokenType.PERCEQ -> "%="
            JavaTokenType.GTGT -> "shr"
            JavaTokenType.LTLT -> "shl"
            JavaTokenType.XOR -> "xor"
            JavaTokenType.AND -> "and"
            JavaTokenType.OR -> "or"
            JavaTokenType.GTGTGT -> "ushr"
            JavaTokenType.GTGTEQ -> "shr"
            JavaTokenType.LTLTEQ -> "shl"
            JavaTokenType.XOREQ -> "xor"
            JavaTokenType.ANDEQ -> "and"
            JavaTokenType.OREQ -> "or"
            JavaTokenType.GTGTGTEQ -> "ushr"
            JavaTokenType.PLUSPLUS -> "++"
            JavaTokenType.MINUSMINUS -> "--"
            JavaTokenType.TILDE -> "~"
            else -> TODO(psiToken.toString())
        }
}

fun JKJavaOperatorToken.toKtToken(): JKKtOperatorToken =
    when (this.psiToken) {
        JavaTokenType.DIV -> JKKtSingleValueOperatorToken(KtTokens.DIV)
        JavaTokenType.MINUS -> JKKtSingleValueOperatorToken(KtTokens.MINUS)
        JavaTokenType.ANDAND -> JKKtSingleValueOperatorToken(KtTokens.ANDAND)
        JavaTokenType.OROR -> JKKtSingleValueOperatorToken(KtTokens.OROR)
        JavaTokenType.PLUS -> JKKtSingleValueOperatorToken(KtTokens.PLUS)
        JavaTokenType.ASTERISK -> JKKtSingleValueOperatorToken(KtTokens.MUL)
        JavaTokenType.GT -> JKKtSingleValueOperatorToken(KtTokens.GT)
        JavaTokenType.GE -> JKKtSingleValueOperatorToken(KtTokens.GTEQ)
        JavaTokenType.LT -> JKKtSingleValueOperatorToken(KtTokens.LT)
        JavaTokenType.LE -> JKKtSingleValueOperatorToken(KtTokens.LTEQ)
        JavaTokenType.PERC -> JKKtSingleValueOperatorToken(KtTokens.PERC)

        JavaTokenType.EQ -> JKKtSingleValueOperatorToken(KtTokens.EQ)
        JavaTokenType.EQEQ -> JKKtSingleValueOperatorToken(KtTokens.EQEQ)
        JavaTokenType.NE -> JKKtSingleValueOperatorToken(KtTokens.EXCLEQ)
        JavaTokenType.PLUSEQ -> JKKtSingleValueOperatorToken(KtTokens.PLUSEQ)
        JavaTokenType.MINUSEQ -> JKKtSingleValueOperatorToken(KtTokens.MINUSEQ)
        JavaTokenType.PLUSPLUS -> JKKtSingleValueOperatorToken(KtTokens.PLUSPLUS)
        JavaTokenType.MINUSMINUS -> JKKtSingleValueOperatorToken(KtTokens.MINUSMINUS)
        JavaTokenType.EXCL -> JKKtSingleValueOperatorToken(KtTokens.EXCL)

        KtTokens.EQEQEQ -> JKKtSingleValueOperatorToken(KtTokens.EQEQEQ)
        KtTokens.EXCLEQEQEQ -> JKKtSingleValueOperatorToken(KtTokens.EXCLEQEQEQ)

        JavaTokenType.AND -> JKKtWordOperatorToken("and")
        JavaTokenType.OR -> JKKtWordOperatorToken("or")
        JavaTokenType.XOR -> JKKtWordOperatorToken("xor")
        JavaTokenType.GTGTGT -> JKKtWordOperatorToken("ushr")
        JavaTokenType.GTGT -> JKKtWordOperatorToken("shr")
        JavaTokenType.LTLT -> JKKtWordOperatorToken("shl")

        JavaTokenType.OREQ -> JKKtWordOperatorToken("or")
        JavaTokenType.ANDEQ -> JKKtWordOperatorToken("and")
        JavaTokenType.LTLTEQ -> JKKtWordOperatorToken("shl")
        JavaTokenType.GTGTEQ -> JKKtWordOperatorToken("shr")
        JavaTokenType.GTGTGTEQ -> JKKtWordOperatorToken("ushr")
        JavaTokenType.XOREQ -> JKKtWordOperatorToken("xor")

        else -> TODO(this.psiToken.toString())
    }


class JKJavaOperatorImpl private constructor(psiToken: IElementType) : JKOperator {
    override val token: JKJavaOperatorToken = JKJavaOperatorToken(psiToken)

    override val precedence: Int
        get() = when (token.psiToken) {
            JavaTokenType.ASTERISK, JavaTokenType.DIV, JavaTokenType.PERC -> 3
            JavaTokenType.PLUS, JavaTokenType.MINUS -> 4
            KtTokens.ELVIS -> 7
            JavaTokenType.GT, JavaTokenType.LT, JavaTokenType.GE, JavaTokenType.LE -> 9
            JavaTokenType.EQEQ, JavaTokenType.NE, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> 10
            JavaTokenType.ANDAND -> 11
            JavaTokenType.OROR -> 12
            JavaTokenType.GTGTGT, JavaTokenType.GTGT, JavaTokenType.LTLT -> 7
            else -> 6 /* simple name */
        }

//    override val priority: Int
//        get() = when (token.psiToken) {
//            JavaTokenType.ASTERISK, JavaTokenType.DIV, JavaTokenType.PERC -> 12
//            JavaTokenType.PLUS, JavaTokenType.MINUS -> 11
//            JavaTokenType.GTGTGT, JavaTokenType.GTGT, JavaTokenType.LTLT -> 10
//            JavaTokenType.GT, JavaTokenType.LT, JavaTokenType.GE, JavaTokenType.LE -> 9
//            JavaTokenType.EQEQ, JavaTokenType.NE, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ ->
//        }

    companion object {
        val tokenToOperator =
            (OPERATION_BIT_SET.types + arrayOf(KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ))
                .associate {
                    it to JKJavaOperatorImpl(it)
                }
    }
}

sealed class JKJavaQualifierImpl : JKQualifier {
    object DOT : JKJavaQualifierImpl()
}

class JKJavaMethodCallExpressionImpl(
    override var identifier: JKMethodSymbol,
    arguments: JKArgumentList,
    typeArgumentList: JKTypeArgumentList = JKTypeArgumentListImpl()
) : JKJavaMethodCallExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethodCallExpression(this, data)

    override var arguments: JKArgumentList by child(arguments)
    override var typeArgumentList: JKTypeArgumentList by child(typeArgumentList)
}

class JKClassBodyImpl(declarations: List<JKDeclaration> = emptyList()) : JKClassBody() {
    override var declarations: List<JKDeclaration> by children(declarations)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassBody(this, data)
}

class JKEmptyClassBodyImpl : JKEmptyClassBody() {
    override var declarations: List<JKDeclaration> by children(emptyList())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitEmptyClassBody(this, data)
}

class JKJavaNewExpressionImpl(
    override var classSymbol: JKClassSymbol,
    arguments: JKArgumentList,
    typeArgumentList: JKTypeArgumentList,
    classBody: JKClassBody = JKEmptyClassBodyImpl()
) : JKJavaNewExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var arguments: JKArgumentList by child(arguments)
    override var typeArgumentList: JKTypeArgumentList by child(typeArgumentList)
    override var classBody: JKClassBody by child(classBody)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewExpression(this, data)
}

class JKJavaNewEmptyArrayImpl(initializer: List<JKExpression>, type: JKTypeElement) : JKJavaNewEmptyArray, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override val type by child(type)
    override var initializer by children(initializer)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewEmptyArray(this, data)
}

class JKJavaNewArrayImpl(initializer: List<JKExpression>, type: JKTypeElement) : JKJavaNewArray, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override val type by child(type)
    override var initializer by children(initializer)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewArray(this, data)
}

class JKJavaPrimitiveTypeImpl(override val jvmPrimitiveType: JvmPrimitiveType) : JKJavaPrimitiveType {
    companion object {
        val BOOLEAN = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.BOOLEAN)
        val CHAR = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.CHAR)
        val BYTE = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.BYTE)
        val SHORT = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.SHORT)
        val INT = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.INT)
        val FLOAT = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.FLOAT)
        val LONG = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.LONG)
        val DOUBLE = JKJavaPrimitiveTypeImpl(JvmPrimitiveType.DOUBLE)

        val KEYWORD_TO_INSTANCE = listOf(
            BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE
        ).associate {
            it.jvmPrimitiveType.javaKeywordName to it
        } + ("void" to JKJavaVoidType)
    }
}

object JKJavaVoidType : JKType {
    override var nullability: Nullability
        get() = Nullability.NotNull
        set(it) {}
}

data class JKJavaArrayTypeImpl(override val type: JKType, override var nullability: Nullability = Nullability.Default) : JKJavaArrayType {
}

class JKJavaDisjunctionTypeImpl(
    override val disjunctions: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKJavaDisjunctionType

class JKReturnStatementImpl(
    expression: JKExpression,
    label: JKLabel = JKLabelEmptyImpl()
) : JKReturnStatement(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitReturnStatement(this, data)

    override val expression by child(expression)
    override var label by child(label)
}

class JKJavaAssertStatementImpl(condition: JKExpression, description: JKExpression) : JKJavaAssertStatement(),
    PsiOwner by PsiOwnerImpl() {
    override val description by child(description)
    override val condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAssertStatement(this, data)
}

class JKJavaForLoopStatementImpl(initializer: JKStatement, condition: JKExpression, updaters: List<JKStatement>, body: JKStatement) :
    JKJavaForLoopStatement(), PsiOwner by PsiOwnerImpl() {
    override var body by child(body)
    override var updaters by children(updaters)
    override var condition by child(condition)
    override var initializer by child(initializer)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaForLoopStatement(this, data)
}


class JKJavaPolyadicExpressionImpl(operands: List<JKExpression>, override var tokens: List<JKOperator>) : JKJavaPolyadicExpression,
    JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var operands by children(operands)

    override fun getTokenBeforeOperand(operand: JKExpression): JKOperator? {
        val index = operands.indexOf(operand)
        return if (index < 1 || index > tokens.size) null else tokens[index - 1]
    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaPolyadicExpression(this, data)
}

class JKJavaAssignmentExpressionImpl(
    field: JKAssignableExpression,
    expression: JKExpression,
    override var operator: JKOperator
) : JKBranchElementBase(), JKJavaAssignmentExpression, PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAssignmentExpression(this, data)
    override var field: JKAssignableExpression by child(field)
    override var expression: JKExpression by child(expression)
}

class JKJavaSwitchStatementImpl(
    expression: JKExpression,
    cases: List<JKJavaSwitchCase>
) : JKJavaSwitchStatement(), PsiOwner by PsiOwnerImpl() {
    override var expression: JKExpression by child(expression)
    override var cases: List<JKJavaSwitchCase> by children(cases)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaSwitchStatement(this, data)
}

class JKJavaDefaultSwitchCaseImpl(statements: List<JKStatement>) : JKJavaDefaultSwitchCase, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override var statements: List<JKStatement> by children(statements)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaDefaultSwitchCase(this, data)
}

class JKJavaLabelSwitchCaseImpl(
    label: JKExpression,
    statements: List<JKStatement>
) : JKJavaLabelSwitchCase, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var statements: List<JKStatement> by children(statements)
    override var label: JKExpression by child(label)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaLabelSwitchCase(this, data)
}

class JKJavaThrowStatementImpl(exception: JKExpression) : JKJavaThrowStatement(), PsiOwner by PsiOwnerImpl() {
    override var exception: JKExpression by child(exception)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaThrowStatement(this, data)
}

class JKJavaTryStatementImpl(
    resourceDeclarations: List<JKDeclaration>,
    tryBlock: JKBlock,
    finallyBlock: JKBlock,
    catchSections: List<JKJavaTryCatchSection>
) : JKJavaTryStatement(), PsiOwner by PsiOwnerImpl() {
    override var resourceDeclarations: List<JKDeclaration> by children(resourceDeclarations)
    override var tryBlock: JKBlock by child(tryBlock)
    override var finallyBlock: JKBlock by child(finallyBlock)
    override var catchSections: List<JKJavaTryCatchSection> by children(catchSections)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaTryStatement(this, data)
}

class JKJavaTryCatchSectionImpl(
    parameter: JKParameter,
    block: JKBlock
) : JKJavaTryCatchSection, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var parameter: JKParameter by child(parameter)
    override var block: JKBlock by child(block)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaTryCatchSection(this, data)
}

class JKJavaSynchronizedStatementImpl(
    lockExpression: JKExpression,
    body: JKBlock
) : JKJavaSynchronizedStatement(), PsiOwner by PsiOwnerImpl() {
    override val lockExpression: JKExpression by child(lockExpression)
    override val body: JKBlock by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaSynchronizedStatement(this, data)
}

class JKJavaAnnotationMethodImpl(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    defaultValue: JKAnnotationMemberValue
) : JKJavaAnnotationMethod(), PsiOwner by PsiOwnerImpl() {
    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children()
    override var defaultValue: JKAnnotationMemberValue by child(defaultValue)
    override var block: JKBlock by child(JKBodyStubImpl)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterListImpl())
    override var annotationList: JKAnnotationList by child(JKAnnotationListImpl())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAnnotationMethod(this, data)
}

class JKKtAnnotationArrayInitializerExpressionImpl(initializers: List<JKAnnotationMemberValue>) : JKKtAnnotationArrayInitializerExpression,
    JKBranchElementBase() {
    constructor(vararg initializers: JKAnnotationMemberValue) : this(initializers.toList())

    override val initializers: List<JKAnnotationMemberValue> by children(initializers)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtAnnotationArrayInitializerExpression(this, data)
}

class JKJavaStaticInitDeclarationImpl(block: JKBlock) : JKJavaStaticInitDeclaration() {
    override var block: JKBlock by child(block)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaStaticInitDeclaration(this, data)
}