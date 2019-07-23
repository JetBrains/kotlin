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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType.BOOLEAN
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType.NULL
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.psi.KtClass

class JKTreeRootImpl(element: JKTreeElement) : JKTreeRoot, JKBranchElementBase() {
    override var element by child(element)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTreeRoot(this, data)
}

class JKFileImpl(
    packageDeclaration: JKPackageDeclaration,
    importList: JKImportList,
    declarationList: List<JKDeclaration>
) : JKFile, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFile(this, data)

    override var packageDeclaration: JKPackageDeclaration by child(packageDeclaration)
    override var importList: JKImportList by child(importList)
    override var declarationList by children(declarationList)
}

class JKClassImpl(
    name: JKNameIdentifier,
    inheritance: JKInheritanceInfo,
    override var classKind: JKClass.ClassKind,
    typeParameterList: JKTypeParameterList,
    classBody: JKClassBody,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKClass(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override var name by child(name)
    override val inheritance by child(inheritance)
    override var typeParameterList: JKTypeParameterList by child(typeParameterList)
    override var classBody: JKClassBody by child(classBody)
    override var annotationList: JKAnnotationList by child(annotationList)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
}

class JKNameIdentifierImpl(override val value: String) : JKNameIdentifier, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNameIdentifier(this, data)
}

class JKForLoopVariableImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    annotationList: JKAnnotationList = JKAnnotationListImpl()
) : JKForLoopVariable() {
    override var initializer by child(initializer)
    override var name by child(name)
    override var type by child(type)
    override var annotationList by child(annotationList)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitForLoopVariable(this, data)
}


class JKParameterImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    override var isVarArgs: Boolean = false,
    initializer: JKExpression = JKStubExpressionImpl(),
    annotationList: JKAnnotationList = JKAnnotationListImpl()
) : JKParameter(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParameter(this, data)

    override var initializer by child(initializer)
    override var name by child(name)
    override var type by child(type)
    override var annotationList by child(annotationList)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock(), PsiOwner by PsiOwnerImpl() {
    constructor(vararg statements: JKStatement) : this(statements.toList())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)

    override var statements by children(statements)
}

class JKBinaryExpressionImpl(
    left: JKExpression,
    right: JKExpression,
    override var operator: JKOperator
) : JKBinaryExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
    override var right by child(right)
    override var left by child(left)
}


class JKPrefixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPrefixExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)

    override var expression by child(expression)
}

class JKPostfixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPostfixExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)

    override var expression by child(expression)
}

class JKExpressionListImpl(expressions: List<JKExpression> = emptyList()) : JKExpressionList, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    constructor(vararg expresions: JKExpression) : this(expresions.asList())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)

    override var expressions by children(expressions)
}

class JKQualifiedExpressionImpl(
    receiver: JKExpression,
    override var operator: JKQualifier,
    selector: JKExpression
) : JKQualifiedExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitQualifiedExpression(this, data)

    override var receiver: JKExpression by child(receiver)
    override var selector: JKExpression by child(selector)
}

class JKExpressionStatementImpl(expression: JKExpression) : JKExpressionStatement(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionStatement(this, data)

    override val expression: JKExpression by child(expression)
}

class JKDeclarationStatementImpl(declaredStatements: List<JKDeclaration>) : JKDeclarationStatement(),
    PsiOwner by PsiOwnerImpl() {
    override val declaredStatements by children(declaredStatements)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDeclarationStatement(this, data)

    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

class JKArrayAccessExpressionImpl(
    expression: JKExpression,
    indexExpression: JKExpression
) : JKArrayAccessExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArrayAccessExpression(this, data)

    override var expression: JKExpression by child(expression)
    override var indexExpression: JKExpression by child(indexExpression)
}

class JKParenthesizedExpressionImpl(expression: JKExpression) : JKParenthesizedExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

    override var expression: JKExpression by child(expression)
}

class JKTypeCastExpressionImpl(expression: JKExpression, type: JKTypeElement) : JKTypeCastExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override var expression by child(expression)
    override var type by child(type)
}

class JKTypeElementImpl(override var type: JKType) : JKTypeElement, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeElement(this, data)
}

data class JKClassTypeImpl(
    override val classReference: JKClassSymbol,
    override val parameters: List<JKType> = emptyList(),
    override val nullability: Nullability = Nullability.Default
) : JKClassType

object JKNoTypeImpl : JKNoType {
    override val nullability: Nullability = Nullability.NotNull
}

class JKStarProjectionTypeImpl : JKStarProjectionType

fun JKType.fqName(): String =
    when (this) {
        is JKClassType -> {
            when (val target = classReference.target) {
                is KtClass -> target.fqName?.asString() ?: throw RuntimeException("FqName can not be calculated")
                is PsiClass -> target.qualifiedName ?: throw RuntimeException("FqName can not be calculated")
                else -> TODO(target.toString())
            }
        }
        is JKJavaPrimitiveType -> jvmPrimitiveType.name
        else -> TODO(toString())
    }

class JKNullLiteral : JKLiteralExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override val literal: String
        get() = "null"
    override val type: LiteralType
        get() = NULL

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLiteralExpression(this, data)
}

class JKBooleanLiteral(val value: Boolean) : JKLiteralExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override val literal: String
        get() = value.toString()
    override val type: LiteralType
        get() = BOOLEAN

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLiteralExpression(this, data)
}

fun JKLiteralExpression.LiteralType.toJkType(symbolProvider: JKSymbolProvider): JKType {
    fun defaultTypeByName(name: String) =
        JKClassTypeImpl(
            symbolProvider.provideClassSymbol("kotlin.$name"), emptyList(), Nullability.NotNull
        )

    return when (this) {
        JKLiteralExpression.LiteralType.CHAR -> defaultTypeByName("Char")
        JKLiteralExpression.LiteralType.BOOLEAN -> defaultTypeByName("Boolean")
        JKLiteralExpression.LiteralType.INT -> defaultTypeByName("Int")
        JKLiteralExpression.LiteralType.LONG -> defaultTypeByName("Long")
        JKLiteralExpression.LiteralType.FLOAT -> defaultTypeByName("Float")
        JKLiteralExpression.LiteralType.DOUBLE -> defaultTypeByName("Double")
        JKLiteralExpression.LiteralType.NULL ->
            ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.unit.toSafe()).toKtClassType(symbolProvider)
        JKLiteralExpression.LiteralType.STRING ->
            ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.string.toSafe()).toKtClassType(symbolProvider)
    }
}

class JKLocalVariableImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    mutabilityElement: JKMutabilityModifierElement,
    annotationList: JKAnnotationList = JKAnnotationListImpl()
) : JKLocalVariable(), PsiOwner by PsiOwnerImpl() {
    override var initializer by child(initializer)
    override var name by child(name)
    override var type by child(type)
    override var annotationList by child(annotationList)

    override var mutabilityElement by child(mutabilityElement)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLocalVariable(this, data)
}

class JKStubExpressionImpl : JKStubExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitStubExpression(this, data)
}

object JKBodyStubImpl : JKBodyStub() {
    override var leftNonCodeElements: List<JKNonCodeElement> = emptyList()//TODO fix
    override var rightNonCodeElements: List<JKNonCodeElement> = emptyList()//TODO fix

    override fun copy(): JKTreeElement = this

    override var statements: List<JKStatement>
        get() = emptyList()
        set(value) {}

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBodyStub(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}

    override var parent: JKElement?
        get() = null
        set (it) {}

    override fun detach(from: JKElement) {
    }

    override fun attach(to: JKElement) {
    }
}

class JKBlockStatementImpl(block: JKBlock) : JKBlockStatement(), PsiOwner by PsiOwnerImpl() {
    override var block by child(block)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlockStatement(this, data)
}

class JKBlockStatementWithoutBracketsImpl(statements: List<JKStatement>) : JKBlockStatementWithoutBrackets(),
    PsiOwner by PsiOwnerImpl() {
    override var statements by children(statements)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlockStatementWithoutBrackets(this, data)
}

class JKThisExpressionImpl(qualifierLabel: JKLabel) : JKThisExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var qualifierLabel: JKLabel by child(qualifierLabel)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitThisExpression(this, data)
}

class JKSuperExpressionImpl(qualifierLabel: JKLabel = JKLabelEmptyImpl()) : JKSuperExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override var qualifierLabel: JKLabel by child(qualifierLabel)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitSuperExpression(this, data)
}

class JKWhileStatementImpl(condition: JKExpression, body: JKStatement) : JKWhileStatement(),
    PsiOwner by PsiOwnerImpl() {
    override var condition by child(condition)
    override var body by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitWhileStatement(this, data)
}

class JKDoWhileStatementImpl(body: JKStatement, condition: JKExpression) : JKDoWhileStatement(),
    PsiOwner by PsiOwnerImpl() {
    override var condition by child(condition)
    override var body by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDoWhileStatement(this, data)
}

class JKBreakStatementImpl : JKBreakStatement(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBreakStatement(this, data)
}

class JKBreakWithLabelStatementImpl(override var label: JKNameIdentifier) : JKBreakWithLabelStatement(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBreakWithLabelStatement(this, data)
}

class JKIfStatementImpl(condition: JKExpression, thenBranch: JKStatement) : JKIfStatement(),
    PsiOwner by PsiOwnerImpl() {
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfStatement(this, data)
}

class JKIfElseStatementImpl(condition: JKExpression, thenBranch: JKStatement, elseBranch: JKStatement) : JKIfElseStatement(),
    PsiOwner by PsiOwnerImpl() {
    override var elseBranch by child(elseBranch)
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfElseStatement(this, data)
}

class JKIfElseExpressionImpl(condition: JKExpression, thenBranch: JKExpression, elseBranch: JKExpression) : JKIfElseExpression,
    JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var elseBranch by child(elseBranch)
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfElseExpression(this, data)
}

class JKClassAccessExpressionImpl(override var identifier: JKClassSymbol) : JKClassAccessExpression, JKElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassAccessExpression(this, data)
}

class JKLambdaExpressionImpl(
    statement: JKStatement,
    parameters: List<JKParameter>,
    functionalType: JKTypeElement = JKTypeElementImpl(JKNoTypeImpl),
    returnType: JKTypeElement = JKTypeElementImpl(JKContextType)//TODO use function type
) : JKLambdaExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var statement by child(statement)
    override val returnType by child(returnType)
    override var parameters by children(parameters)
    override var functionalType by child(functionalType)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLambdaExpression(this, data)
}

class JKInheritanceInfoImpl(
    extends: List<JKTypeElement>,
    implements: List<JKTypeElement>
) : JKInheritanceInfo, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var extends: List<JKTypeElement> by children(extends)
    override var implements: List<JKTypeElement> by children(implements)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitInheritanceInfo(this, data)
}


class JKDelegationConstructorCallImpl(
    override val identifier: JKMethodSymbol,
    expression: JKExpression,
    arguments: JKArgumentList
) : JKBranchElementBase(), JKDelegationConstructorCall, PsiOwner by PsiOwnerImpl() {
    override var typeArgumentList: JKTypeArgumentList by child(JKTypeArgumentListImpl())
    override val expression: JKExpression by child(expression)
    override var arguments: JKArgumentList by child(arguments)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDelegationConstructorCall(this, data)
}

class JKFieldAccessExpressionImpl(override var identifier: JKFieldSymbol) : JKFieldAccessExpression, JKElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFieldAccessExpression(this, data)
}

class JKPackageAccessExpressionImpl(override var identifier: JKPackageSymbol) : JKPackageAccessExpression, JKElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPackageAccessExpression(this, data)
}


val JKStatement.statements: List<JKStatement>
    get() =
        when (this) {
            is JKBlockStatement -> block.statements
            else -> listOf(this)
        }

class JKLabelEmptyImpl : JKLabelEmpty, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabelEmpty(this, data)
}

class JKLabelTextImpl(label: JKNameIdentifier) : JKLabelText, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val label: JKNameIdentifier by child(label)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabelText(this, data)
}

class JKContinueStatementImpl(label: JKLabel) : JKContinueStatement(), PsiOwner by PsiOwnerImpl() {
    override var label: JKLabel by child(label)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitContinueStatement(this, data)
}

class JKLabeledStatementImpl(statement: JKStatement, labels: List<JKNameIdentifier>) : JKLabeledStatement, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override var statement: JKStatement by child(statement)
    override val labels: List<JKNameIdentifier> by children(labels)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabeledStatement(this, data)
}

class JKEmptyStatementImpl : JKEmptyStatement(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitEmptyStatement(this, data)
}

class PsiOwnerImpl(override var psi: PsiElement? = null) : PsiOwner

val JKElement.psi: PsiElement?
    get() = (this as? PsiOwner)?.psi

inline fun <reified Elem : PsiElement> JKElement.psi(): Elem? = (this as? PsiOwner)?.psi as? Elem

class JKTypeParameterListImpl(typeParameters: List<JKTypeParameter> = emptyList()) : JKTypeParameterList, JKBranchElementBase() {
    override var typeParameters by children(typeParameters)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeParameterList(this, data)
}

class JKTypeParameterImpl(name: JKNameIdentifier, upperBounds: List<JKTypeElement>) : JKTypeParameter, JKBranchElementBase() {
    override var name: JKNameIdentifier by child(name)
    override var upperBounds: List<JKTypeElement> by children(upperBounds)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeParameter(this, data)
}

data class JKVarianceTypeParameterTypeImpl(
    override val variance: JKVarianceTypeParameterType.Variance,
    override val boundType: JKType
) : JKVarianceTypeParameterType

data class JKTypeParameterTypeImpl(
    override val name: String,
    override val nullability: Nullability = Nullability.Default
) : JKTypeParameterType

class JKEnumConstantImpl(
    name: JKNameIdentifier,
    arguments: JKArgumentList,
    body: JKClassBody,
    type: JKTypeElement,
    annotationList: JKAnnotationList = JKAnnotationListImpl()
) : JKEnumConstant(), PsiOwner by PsiOwnerImpl() {
    override var name: JKNameIdentifier by child(name)
    override val arguments: JKArgumentList by child(arguments)
    override val body: JKClassBody by child(body)
    override var type: JKTypeElement by child(type)
    override var initializer: JKExpression by child(JKStubExpressionImpl())
    override var annotationList by child(annotationList)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitEnumConstant(this, data)
}

fun JKTypeElement.present(): Boolean =
    type != JKNoTypeImpl

class JKForInStatementImpl(declaration: JKDeclaration, iterationExpression: JKExpression, body: JKStatement) :
    JKForInStatement(), PsiOwner by PsiOwnerImpl() {
    override var declaration: JKDeclaration by child(declaration)
    override var iterationExpression: JKExpression by child(iterationExpression)
    override var body: JKStatement by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitForInStatement(this, data)
}

fun JKStatement.isEmpty(): Boolean =
    when (this) {
        is JKEmptyStatement -> true
        is JKBlockStatement -> block is JKBodyStubImpl
        is JKExpressionStatement -> expression is JKStubExpression
        else -> false
    }

class JKPackageDeclarationImpl(packageName: JKNameIdentifier) : JKPackageDeclaration() {
    override var packageName: JKNameIdentifier by child(packageName)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPackageDeclaration(this, data)
}

class JKAnnotationListImpl(annotations: List<JKAnnotation> = emptyList()) : JKAnnotationList, JKBranchElementBase() {
    override var annotations: List<JKAnnotation> by children(annotations)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitAnnotationList(this, data)
}

class JKAnnotationImpl(
    override var classSymbol: JKClassSymbol,
    arguments: List<JKAnnotationParameter> = emptyList()
) : JKAnnotation, JKBranchElementBase() {
    override var arguments: List<JKAnnotationParameter> by children(arguments)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitAnnotation(this, data)

    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

class JKTypeArgumentListImpl(typeArguments: List<JKTypeElement> = emptyList()) : JKTypeArgumentList, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override val typeArguments: List<JKTypeElement> by children(typeArguments)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeArgumentList(this, data)

}

class JKClassLiteralExpressionImpl(
    classType: JKTypeElement,
    override var literalType: JKClassLiteralExpression.LiteralType
) : JKClassLiteralExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val classType: JKTypeElement by child(classType)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassLiteralExpression(this, data)
}

class JKImportStatementImpl(name: JKNameIdentifier) : JKImportStatement, JKBranchElementBase() {
    override val name: JKNameIdentifier by child(name)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitImportStatement(this, data)

    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

class JKImportListImpl(imports: List<JKImportStatement>) : JKImportList, JKBranchElementBase() {
    override var imports by children(imports)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitImportList(this, data)
}


class JKAnnotationParameterImpl(value: JKAnnotationMemberValue) : JKAnnotationParameter, JKBranchElementBase() {
    override var value: JKAnnotationMemberValue by child(value)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitAnnotationParameter(this, data)
}

class JKAnnotationNameParameterImpl(
    value: JKAnnotationMemberValue,
    name: JKNameIdentifier
) : JKAnnotationNameParameter, JKBranchElementBase() {
    override var value: JKAnnotationMemberValue by child(value)
    override val name: JKNameIdentifier by child(name)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitAnnotationNameParameter(this, data)
}

class JKNamedArgumentImpl(
    value: JKExpression,
    name: JKNameIdentifier
) : JKNamedArgument, JKBranchElementBase() {
    override var value by child(value)
    override val name by child(name)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNamedArgument(this, data)
}

class JKArgumentImpl(value: JKExpression) : JKArgument, JKBranchElementBase() {
    override var value by child(value)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArgument(this, data)
}

class JKArgumentListImpl(arguments: List<JKArgument> = emptyList()) : JKArgumentList, JKBranchElementBase() {
    constructor(vararg arguments: JKArgument) : this(arguments.toList())
    constructor(vararg values: JKExpression) : this(values.map { JKArgumentImpl(it) })

    override var arguments by children(arguments)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArgumentList(this, data)
}


class JKMutabilityModifierElementImpl(override var mutability: Mutability) : JKMutabilityModifierElement() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitMutabilityModifierElement(this, data)
}

class JKModalityModifierElementImpl(override var modality: Modality) : JKModalityModifierElement() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModalityModifierElement(this, data)
}

class JKVisibilityModifierElementImpl(override var visibility: Visibility) : JKVisibilityModifierElement() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitVisibilityModifierElement(this, data)
}

class JKOtherModifierElementImpl(override var otherModifier: OtherModifier) : JKOtherModifierElement() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExtraModifierElement(this, data)
}

class JKMethodReferenceExpressionImpl(
    qualifier: JKExpression,
    override val identifier: JKNamedSymbol,
    functionalType: JKTypeElement,
    override val isConstructorCall: Boolean
) : JKMethodReferenceExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val qualifier by child(qualifier)
    override val functionalType by child(functionalType)


    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitMethodReferenceExpression(this, data)
}