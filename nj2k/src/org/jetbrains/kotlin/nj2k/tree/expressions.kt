/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.symbols.*


import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.nj2k.types.*

abstract class JKExpression : JKAnnotationMemberValue(), PsiOwner by PsiOwnerImpl() {
    // we don't need exact type here (eg with substituted type parameters)
    abstract fun calculateType(typeFactory: JKTypeFactory): JKType?
}

abstract class JKOperatorExpression : JKExpression() {
    abstract var operator: JKOperator
    override fun calculateType(typeFactory: JKTypeFactory) = operator.returnType
}

class JKBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    override var operator: JKOperator
) : JKOperatorExpression() {
    var left by child(left)
    var right by child(right)

    override fun accept(visitor: JKVisitor) = visitor.visitBinaryExpression(this)
}

abstract class JKUnaryExpression : JKOperatorExpression() {
    abstract var expression: JKExpression
}

class JKPrefixExpression(expression: JKExpression, override var operator: JKOperator) : JKUnaryExpression() {
    override var expression by child(expression)
    override fun accept(visitor: JKVisitor) = visitor.visitPrefixExpression(this)
}

class JKPostfixExpression(expression: JKExpression, override var operator: JKOperator) : JKUnaryExpression() {
    override var expression by child(expression)
    override fun accept(visitor: JKVisitor) = visitor.visitPostfixExpression(this)
}

class JKQualifiedExpression(
    receiver: JKExpression,
    selector: JKExpression
) : JKExpression() {
    var receiver: JKExpression by child(receiver)
    var selector: JKExpression by child(selector)
    override fun calculateType(typeFactory: JKTypeFactory) = selector.calculateType(typeFactory)
    override fun accept(visitor: JKVisitor) = visitor.visitQualifiedExpression(this)
}

class JKParenthesizedExpression(expression: JKExpression) : JKExpression() {
    var expression: JKExpression by child(expression)
    override fun calculateType(typeFactory: JKTypeFactory) = expression.calculateType(typeFactory)
    override fun accept(visitor: JKVisitor) = visitor.visitParenthesizedExpression(this)
}

class JKTypeCastExpression(expression: JKExpression, type: JKTypeElement) : JKExpression() {
    var expression by child(expression)
    var type by child(type)
    override fun calculateType(typeFactory: JKTypeFactory) = type.type
    override fun accept(visitor: JKVisitor) = visitor.visitTypeCastExpression(this)
}

class JKLiteralExpression(
    var literal: String,
    val type: LiteralType
) : JKExpression() {
    override fun accept(visitor: JKVisitor) = visitor.visitLiteralExpression(this)

    override fun calculateType(typeFactory: JKTypeFactory) = when (type) {
        LiteralType.CHAR -> typeFactory.types.char
        LiteralType.BOOLEAN -> typeFactory.types.boolean
        LiteralType.INT -> typeFactory.types.int
        LiteralType.LONG -> typeFactory.types.long
        LiteralType.FLOAT -> typeFactory.types.float
        LiteralType.DOUBLE -> typeFactory.types.double
        LiteralType.NULL -> typeFactory.types.nullableAny
        LiteralType.STRING -> typeFactory.types.string
    }

    enum class LiteralType {
        STRING, CHAR, BOOLEAN, NULL, INT, LONG, FLOAT, DOUBLE
    }
}

class JKStubExpression : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitStubExpression(this)
}

class JKThisExpression(qualifierLabel: JKLabel, private val type: JKType) : JKExpression() {
    var qualifierLabel: JKLabel by child(qualifierLabel)
    override fun calculateType(typeFactory: JKTypeFactory) = type
    override fun accept(visitor: JKVisitor) = visitor.visitThisExpression(this)
}

class JKSuperExpression(qualifierLabel: JKLabel, private val type: JKType) : JKExpression() {
    var qualifierLabel: JKLabel by child(qualifierLabel)
    override fun calculateType(typeFactory: JKTypeFactory) = type
    override fun accept(visitor: JKVisitor) = visitor.visitSuperExpression(this)
}

class JKIfElseExpression(
    condition: JKExpression,
    thenBranch: JKExpression,
    elseBranch: JKExpression,
    private val type: JKType
) : JKExpression() {
    var condition by child(condition)
    var thenBranch by child(thenBranch)
    var elseBranch by child(elseBranch)

    override fun calculateType(typeFactory: JKTypeFactory): JKType? = type
    override fun accept(visitor: JKVisitor) = visitor.visitIfElseExpression(this)
}

class JKLambdaExpression(
    statement: JKStatement,
    parameters: List<JKParameter>,
    functionalType: JKTypeElement = JKTypeElement(JKNoType),
    returnType: JKTypeElement = JKTypeElement(JKContextType)
) : JKExpression() {
    var statement by child(statement)
    var parameters by children(parameters)
    var functionalType by child(functionalType)
    val returnType by child(returnType)

    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitLambdaExpression(this)
}


abstract class JKCallExpression : JKExpression(), JKTypeArgumentListOwner {
    abstract val identifier: JKMethodSymbol
    abstract var arguments: JKArgumentList
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = identifier.returnType
}

class JKDelegationConstructorCall(
    override val identifier: JKMethodSymbol,
    expression: JKExpression,
    arguments: JKArgumentList
) : JKCallExpression() {
    override var typeArgumentList: JKTypeArgumentList by child(JKTypeArgumentList())
    val expression: JKExpression by child(expression)
    override var arguments: JKArgumentList by child(arguments)
    override fun accept(visitor: JKVisitor) = visitor.visitDelegationConstructorCall(this)
}

class JKCallExpressionImpl(
    override val identifier: JKMethodSymbol,
    arguments: JKArgumentList,
    typeArgumentList: JKTypeArgumentList = JKTypeArgumentList()
) : JKCallExpression() {
    override var typeArgumentList by child(typeArgumentList)
    override var arguments by child(arguments)
    override fun accept(visitor: JKVisitor) = visitor.visitCallExpressionImpl(this)
}

class JKNewExpression(
    val classSymbol: JKClassSymbol,
    arguments: JKArgumentList,
    typeArgumentList: JKTypeArgumentList,
    classBody: JKClassBody = JKClassBody(),
    val isAnonymousClass: Boolean = false
) : JKExpression() {
    var typeArgumentList by child(typeArgumentList)
    var arguments by child(arguments)
    var classBody by child(classBody)
    override fun calculateType(typeFactory: JKTypeFactory) = classSymbol.asType()
    override fun accept(visitor: JKVisitor) = visitor.visitNewExpression(this)
}


class JKFieldAccessExpression(var identifier: JKFieldSymbol) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory) = identifier.fieldType
    override fun accept(visitor: JKVisitor) = visitor.visitFieldAccessExpression(this)
}

class JKPackageAccessExpression(var identifier: JKPackageSymbol) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitPackageAccessExpression(this)
}


class JKClassAccessExpression(var identifier: JKClassSymbol) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitClassAccessExpression(this)
}

class JKMethodAccessExpression(val identifier: JKMethodSymbol) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitMethodAccessExpression(this)
}

class JKTypeQualifierExpression(val type: JKType) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitTypeQualifierExpression(this)
}

class JKMethodReferenceExpression(
    qualifier: JKExpression,
    val identifier: JKSymbol,
    functionalType: JKTypeElement,
    val isConstructorCall: Boolean
) : JKExpression() {
    val qualifier by child(qualifier)
    val functionalType by child(functionalType)
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitMethodReferenceExpression(this)
}


class JKLabeledExpression(statement: JKStatement, labels: List<JKNameIdentifier>) : JKExpression() {
    var statement: JKStatement by child(statement)
    val labels: List<JKNameIdentifier> by children(labels)
    override fun calculateType(typeFactory: JKTypeFactory) = typeFactory.types.unit
    override fun accept(visitor: JKVisitor) = visitor.visitLabeledExpression(this)
}

class JKClassLiteralExpression(
    classType: JKTypeElement,
    var literalType: ClassLiteralType
) : JKExpression() {
    val classType: JKTypeElement by child(classType)
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = when (literalType) {
        ClassLiteralType.KOTLIN_CLASS -> typeFactory.types.kotlinClass
        else -> typeFactory.types.javaKlass
    }

    override fun accept(visitor: JKVisitor) = visitor.visitClassLiteralExpression(this)

    enum class ClassLiteralType {
        KOTLIN_CLASS,
        JAVA_CLASS,
        JAVA_PRIMITIVE_CLASS,
        JAVA_VOID_TYPE
    }
}


abstract class JKKtAssignmentChainLink : JKExpression() {
    abstract val receiver: JKExpression
    abstract val assignmentStatement: JKKtAssignmentStatement
    abstract val field: JKExpression
    override fun calculateType(typeFactory: JKTypeFactory) = field.calculateType(typeFactory)
}

class JKAssignmentChainAlsoLink(
    receiver: JKExpression,
    assignmentStatement: JKKtAssignmentStatement,
    field: JKExpression
) : JKKtAssignmentChainLink() {
    override val receiver by child(receiver)
    override val assignmentStatement by child(assignmentStatement)
    override val field by child(field)
    override fun accept(visitor: JKVisitor) = visitor.visitAssignmentChainAlsoLink(this)
}

class JKAssignmentChainLetLink(
    receiver: JKExpression,
    assignmentStatement: JKKtAssignmentStatement,
    field: JKExpression
) : JKKtAssignmentChainLink() {
    override val receiver by child(receiver)
    override val assignmentStatement by child(assignmentStatement)
    override val field by child(field)
    override fun accept(visitor: JKVisitor) = visitor.visitAssignmentChainLetLink(this)
}


class JKIsExpression(expression: JKExpression, type: JKTypeElement) : JKExpression() {
    var type by child(type)
    var expression by child(expression)
    override fun calculateType(typeFactory: JKTypeFactory) = typeFactory.types.boolean
    override fun accept(visitor: JKVisitor) = visitor.visitIsExpression(this)
}

class JKKtThrowExpression(exception: JKExpression) : JKExpression() {
    var exception: JKExpression by child(exception)
    override fun calculateType(typeFactory: JKTypeFactory) = typeFactory.types.nothing
    override fun accept(visitor: JKVisitor) = visitor.visitKtThrowExpression(this)
}

class JKKtItExpression(val type: JKType) : JKExpression() {
    override fun calculateType(typeFactory: JKTypeFactory) = type
    override fun accept(visitor: JKVisitor) = visitor.visitKtItExpression(this)
}

class JKKtAnnotationArrayInitializerExpression(initializers: List<JKAnnotationMemberValue>) : JKExpression() {
    constructor(vararg initializers: JKAnnotationMemberValue) : this(initializers.toList())

    val initializers: List<JKAnnotationMemberValue> by children(initializers)
    override fun calculateType(typeFactory: JKTypeFactory): JKType? = null
    override fun accept(visitor: JKVisitor) = visitor.visitKtAnnotationArrayInitializerExpression(this)
}

class JKKtTryExpression(
    tryBlock: JKBlock,
    finallyBlock: JKBlock,
    catchSections: List<JKKtTryCatchSection>
) : JKExpression() {
    var tryBlock: JKBlock by child(tryBlock)
    var finallyBlock: JKBlock by child(finallyBlock)
    var catchSections: List<JKKtTryCatchSection> by children(catchSections)
    override fun calculateType(typeFactory: JKTypeFactory) =
        typeFactory.types.unit // as converted from Java try statement

    override fun accept(visitor: JKVisitor) = visitor.visitKtTryExpression(this)
}

class JKJavaNewEmptyArray(initializer: List<JKExpression>, type: JKTypeElement) : JKExpression() {
    val type by child(type)
    var initializer by children(initializer)
    override fun calculateType(typeFactory: JKTypeFactory) = type.type
    override fun accept(visitor: JKVisitor) = visitor.visitJavaNewEmptyArray(this)
}

class JKJavaNewArray(initializer: List<JKExpression>, type: JKTypeElement) : JKExpression() {
    val type by child(type)
    var initializer by children(initializer)
    override fun calculateType(typeFactory: JKTypeFactory) = type.type
    override fun accept(visitor: JKVisitor) = visitor.visitJavaNewArray(this)
}


class JKJavaAssignmentExpression(
    field: JKExpression,
    expression: JKExpression,
    var operator: JKOperator
) : JKExpression() {
    var field by child(field)
    var expression by child(expression)
    override fun calculateType(typeFactory: JKTypeFactory) = field.calculateType(typeFactory)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaAssignmentExpression(this)
}
