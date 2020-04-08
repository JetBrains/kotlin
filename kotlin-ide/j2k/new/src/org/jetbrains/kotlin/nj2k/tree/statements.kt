/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor

abstract class JKStatement : JKTreeElement(), PsiOwner by PsiOwnerImpl()

class JKEmptyStatement : JKStatement() {
    override fun accept(visitor: JKVisitor) = visitor.visitEmptyStatement(this)
}

abstract class JKLoopStatement : JKStatement() {
    abstract var body: JKStatement
}

class JKWhileStatement(condition: JKExpression, body: JKStatement) : JKLoopStatement() {
    var condition by child(condition)
    override var body by child(body)
    override fun accept(visitor: JKVisitor) = visitor.visitWhileStatement(this)
}

class JKDoWhileStatement(body: JKStatement, condition: JKExpression) : JKLoopStatement() {
    var condition by child(condition)
    override var body by child(body)
    override fun accept(visitor: JKVisitor) = visitor.visitDoWhileStatement(this)
}

class JKForInStatement(declaration: JKDeclaration, iterationExpression: JKExpression, body: JKStatement) : JKStatement() {
    var declaration: JKDeclaration by child(declaration)
    var iterationExpression: JKExpression by child(iterationExpression)
    var body: JKStatement by child(body)
    override fun accept(visitor: JKVisitor) = visitor.visitForInStatement(this)
}

class JKIfElseStatement(condition: JKExpression, thenBranch: JKStatement, elseBranch: JKStatement) : JKStatement() {
    var condition by child(condition)
    var thenBranch by child(thenBranch)
    var elseBranch by child(elseBranch)
    override fun accept(visitor: JKVisitor) = visitor.visitIfElseStatement(this)
}

class JKBreakStatement(label: JKLabel) : JKStatement() {
    val label: JKLabel by child(label)
    override fun accept(visitor: JKVisitor) = visitor.visitBreakStatement(this)
}

class JKContinueStatement(label: JKLabel) : JKStatement() {
    var label: JKLabel by child(label)
    override fun accept(visitor: JKVisitor) = visitor.visitContinueStatement(this)
}

class JKBlockStatement(block: JKBlock) : JKStatement() {
    var block by child(block)
    override fun accept(visitor: JKVisitor) = visitor.visitBlockStatement(this)
}

class JKBlockStatementWithoutBrackets(statements: List<JKStatement>) : JKStatement() {
    var statements by children(statements)
    override fun accept(visitor: JKVisitor) = visitor.visitBlockStatementWithoutBrackets(this)
}

class JKExpressionStatement(expression: JKExpression) : JKStatement() {
    var expression: JKExpression by child(expression)
    override fun accept(visitor: JKVisitor) = visitor.visitExpressionStatement(this)
}

class JKDeclarationStatement(declaredStatements: List<JKDeclaration>) : JKStatement() {
    val declaredStatements by children(declaredStatements)
    override fun accept(visitor: JKVisitor) = visitor.visitDeclarationStatement(this)
}

class JKKtWhenStatement(
    expression: JKExpression,
    cases: List<JKKtWhenCase>
) : JKStatement() {
    var expression: JKExpression by child(expression)
    var cases: List<JKKtWhenCase> by children(cases)
    override fun accept(visitor: JKVisitor) = visitor.visitKtWhenStatement(this)
}

class JKKtConvertedFromForLoopSyntheticWhileStatement(
    variableDeclarations: List<JKStatement>,
    whileStatement: JKWhileStatement
) : JKStatement() {
    var variableDeclarations: List<JKStatement> by children(variableDeclarations)
    var whileStatement: JKWhileStatement by child(whileStatement)
    override fun accept(visitor: JKVisitor) = visitor.visitKtConvertedFromForLoopSyntheticWhileStatement(this)
}

class JKKtAssignmentStatement(
    field: JKExpression,
    expression: JKExpression,
    var token: JKOperatorToken
) : JKStatement() {
    var field: JKExpression by child(field)
    var expression by child(expression)
    override fun accept(visitor: JKVisitor) = visitor.visitKtAssignmentStatement(this)
}

class JKReturnStatement(
    expression: JKExpression,
    label: JKLabel = JKLabelEmpty()
) : JKStatement() {
    val expression by child(expression)
    var label by child(label)
    override fun accept(visitor: JKVisitor) = visitor.visitReturnStatement(this)
}

class JKJavaSwitchStatement(
    expression: JKExpression,
    cases: List<JKJavaSwitchCase>
) : JKStatement() {
    var expression: JKExpression by child(expression)
    var cases: List<JKJavaSwitchCase> by children(cases)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaSwitchStatement(this)
}

class JKJavaThrowStatement(exception: JKExpression) : JKStatement() {
    var exception: JKExpression by child(exception)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaThrowStatement(this)
}

class JKJavaTryStatement(
    resourceDeclarations: List<JKDeclaration>,
    tryBlock: JKBlock,
    finallyBlock: JKBlock,
    catchSections: List<JKJavaTryCatchSection>
) : JKStatement() {
    var resourceDeclarations: List<JKDeclaration> by children(resourceDeclarations)
    var tryBlock: JKBlock by child(tryBlock)
    var finallyBlock: JKBlock by child(finallyBlock)
    var catchSections: List<JKJavaTryCatchSection> by children(catchSections)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaTryStatement(this)
}

class JKJavaSynchronizedStatement(
    lockExpression: JKExpression,
    body: JKBlock
) : JKStatement() {
    val lockExpression: JKExpression by child(lockExpression)
    val body: JKBlock by child(body)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaSynchronizedStatement(this)
}


class JKJavaAssertStatement(condition: JKExpression, description: JKExpression) : JKStatement() {
    val description by child(description)
    val condition by child(condition)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaAssertStatement(this)
}

class JKJavaForLoopStatement(
    initializers: List<JKStatement>,
    condition: JKExpression,
    updaters: List<JKStatement>,
    body: JKStatement
) : JKLoopStatement() {
    override var body by child(body)
    var updaters by children(updaters)
    var condition by child(condition)
    var initializers by children(initializers)

    override fun accept(visitor: JKVisitor) = visitor.visitJavaForLoopStatement(this)
}

class JKJavaAnnotationMethod(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    defaultValue: JKAnnotationMemberValue,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKMethod(), JKAnnotationListOwner, JKTypeParameterListOwner {
    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children()
    var defaultValue: JKAnnotationMemberValue by child(defaultValue)
    override var block: JKBlock by child(JKBodyStub)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterList())
    override var annotationList: JKAnnotationList by child(JKAnnotationList())
    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaAnnotationMethod(this)
}


