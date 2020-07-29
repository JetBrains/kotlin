/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol

import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.nj2k.types.JKType

class JKTreeRoot(element: JKTreeElement) : JKTreeElement() {
    var element by child(element)
    override fun accept(visitor: JKVisitor) = visitor.visitTreeRoot(this)
}

class JKFile(
    packageDeclaration: JKPackageDeclaration,
    importList: JKImportList,
    declarationList: List<JKDeclaration>
) : JKTreeElement(), PsiOwner by PsiOwnerImpl() {
    override fun accept(visitor: JKVisitor) = visitor.visitFile(this)

    var packageDeclaration: JKPackageDeclaration by child(packageDeclaration)
    var importList: JKImportList by child(importList)
    var declarationList by children(declarationList)
}

class JKTypeElement(var type: JKType, annotationList: JKAnnotationList = JKAnnotationList()) : JKTreeElement(), JKAnnotationListOwner {
    override fun accept(visitor: JKVisitor) = visitor.visitTypeElement(this)
    override var annotationList: JKAnnotationList by child(annotationList)
}

abstract class JKBlock : JKTreeElement() {
    abstract var statements: List<JKStatement>

    val leftBrace = JKTokenElementImpl("{")
    val rightBrace = JKTokenElementImpl("}")
}


object JKBodyStub : JKBlock() {
    override val trailingComments: MutableList<JKComment> = mutableListOf()
    override val leadingComments: MutableList<JKComment> = mutableListOf()
    override var hasTrailingLineBreak = false
    override var hasLeadingLineBreak = false

    override fun copy(): JKTreeElement = this

    override var statements: List<JKStatement>
        get() = emptyList()
        set(_) {}

    override fun acceptChildren(visitor: JKVisitor) {}

    override var parent: JKElement?
        get() = null
        set(_) {}

    override fun detach(from: JKElement) {}
    override fun attach(to: JKElement) {}
    override fun accept(visitor: JKVisitor) = Unit
}


class JKInheritanceInfo(
    extends: List<JKTypeElement>,
    implements: List<JKTypeElement>
) : JKTreeElement() {
    var extends: List<JKTypeElement> by children(extends)
    var implements: List<JKTypeElement> by children(implements)

    override fun accept(visitor: JKVisitor) = visitor.visitInheritanceInfo(this)
}

class JKPackageDeclaration(name: JKNameIdentifier) : JKDeclaration() {
    override var name: JKNameIdentifier by child(name)
    override fun accept(visitor: JKVisitor) = visitor.visitPackageDeclaration(this)
}

abstract class JKLabel : JKTreeElement()

class JKLabelEmpty : JKLabel() {
    override fun accept(visitor: JKVisitor) = visitor.visitLabelEmpty(this)
}

class JKLabelText(label: JKNameIdentifier) : JKLabel() {
    val label: JKNameIdentifier by child(label)
    override fun accept(visitor: JKVisitor) = visitor.visitLabelText(this)
}

class JKImportStatement(name: JKNameIdentifier) : JKTreeElement() {
    val name: JKNameIdentifier by child(name)
    override fun accept(visitor: JKVisitor) = visitor.visitImportStatement(this)
}

class JKImportList(imports: List<JKImportStatement>) : JKTreeElement() {
    var imports by children(imports)
    override fun accept(visitor: JKVisitor) = visitor.visitImportList(this)
}

abstract class JKAnnotationParameter : JKTreeElement() {
    abstract var value: JKAnnotationMemberValue
}

class JKAnnotationParameterImpl(value: JKAnnotationMemberValue) : JKAnnotationParameter() {
    override var value: JKAnnotationMemberValue by child(value)

    override fun accept(visitor: JKVisitor) = visitor.visitAnnotationParameter(this)
}

class JKAnnotationNameParameter(
    value: JKAnnotationMemberValue,
    name: JKNameIdentifier
) : JKAnnotationParameter() {
    override var value: JKAnnotationMemberValue by child(value)
    val name: JKNameIdentifier by child(name)
    override fun accept(visitor: JKVisitor) = visitor.visitAnnotationNameParameter(this)
}

abstract class JKArgument : JKTreeElement() {
    abstract var value: JKExpression
}

class JKNamedArgument(
    value: JKExpression,
    name: JKNameIdentifier
) : JKArgument() {
    override var value by child(value)
    val name by child(name)
    override fun accept(visitor: JKVisitor) = visitor.visitNamedArgument(this)
}

class JKArgumentImpl(value: JKExpression) : JKArgument() {
    override var value by child(value)
    override fun accept(visitor: JKVisitor) = visitor.visitArgument(this)
}

class JKArgumentList(arguments: List<JKArgument> = emptyList()) : JKTreeElement() {
    constructor(vararg arguments: JKArgument) : this(arguments.toList())
    constructor(vararg values: JKExpression) : this(values.map { JKArgumentImpl(it) })

    var arguments by children(arguments)
    override fun accept(visitor: JKVisitor) = visitor.visitArgumentList(this)
}


class JKTypeParameterList(typeParameters: List<JKTypeParameter> = emptyList()) : JKTreeElement() {
    var typeParameters by children(typeParameters)
    override fun accept(visitor: JKVisitor) = visitor.visitTypeParameterList(this)
}


class JKAnnotationList(annotations: List<JKAnnotation> = emptyList()) : JKTreeElement() {
    var annotations: List<JKAnnotation> by children(annotations)
    override fun accept(visitor: JKVisitor) = visitor.visitAnnotationList(this)
}

class JKAnnotation(
    var classSymbol: JKClassSymbol,
    arguments: List<JKAnnotationParameter> = emptyList()
) : JKAnnotationMemberValue() {
    var arguments: List<JKAnnotationParameter> by children(arguments)
    override fun accept(visitor: JKVisitor) = visitor.visitAnnotation(this)
}

class JKTypeArgumentList(typeArguments: List<JKTypeElement> = emptyList()) : JKTreeElement(), PsiOwner by PsiOwnerImpl() {
    var typeArguments: List<JKTypeElement> by children(typeArguments)
    override fun accept(visitor: JKVisitor) = visitor.visitTypeArgumentList(this)
}

class JKNameIdentifier(val value: String) : JKTreeElement() {
    override fun accept(visitor: JKVisitor) = visitor.visitNameIdentifier(this)
}


interface JKAnnotationListOwner : JKFormattingOwner {
    var annotationList: JKAnnotationList
}


class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock() {
    constructor(vararg statements: JKStatement) : this(statements.toList())

    override var statements by children(statements)
    override fun accept(visitor: JKVisitor) = visitor.visitBlock(this)
}

class JKKtWhenCase(labels: List<JKKtWhenLabel>, statement: JKStatement) : JKTreeElement() {
    var labels: List<JKKtWhenLabel> by children(labels)
    var statement: JKStatement by child(statement)
    override fun accept(visitor: JKVisitor) = visitor.visitKtWhenCase(this)
}

abstract class JKKtWhenLabel : JKTreeElement()

class JKKtElseWhenLabel : JKKtWhenLabel() {
    override fun accept(visitor: JKVisitor) = visitor.visitKtElseWhenLabel(this)
}

class JKKtValueWhenLabel(expression: JKExpression) : JKKtWhenLabel() {
    var expression: JKExpression by child(expression)
    override fun accept(visitor: JKVisitor) = visitor.visitKtValueWhenLabel(this)
}


class JKClassBody(declarations: List<JKDeclaration> = emptyList()) : JKTreeElement() {
    var declarations: List<JKDeclaration> by children(declarations)
    override fun accept(visitor: JKVisitor) = visitor.visitClassBody(this)

    val leftBrace = JKTokenElementImpl("{")
    val rightBrace = JKTokenElementImpl("}")
}


class JKJavaTryCatchSection(
    parameter: JKParameter,
    block: JKBlock
) : JKStatement() {
    var parameter: JKParameter by child(parameter)
    var block: JKBlock by child(block)
    override fun accept(visitor: JKVisitor) = visitor.visitJavaTryCatchSection(this)
}

abstract class JKJavaSwitchCase : JKTreeElement() {
    abstract fun isDefault(): Boolean
    abstract var statements: List<JKStatement>
}

class JKJavaDefaultSwitchCase(statements: List<JKStatement>) : JKJavaSwitchCase(), PsiOwner by PsiOwnerImpl() {
    override var statements: List<JKStatement> by children(statements)
    override fun isDefault(): Boolean = true
    override fun accept(visitor: JKVisitor) = visitor.visitJavaDefaultSwitchCase(this)
}

class JKJavaLabelSwitchCase(
    label: JKExpression,
    statements: List<JKStatement>
) : JKJavaSwitchCase(), PsiOwner by PsiOwnerImpl() {
    override var statements: List<JKStatement> by children(statements)
    var label: JKExpression by child(label)
    override fun isDefault(): Boolean = false
    override fun accept(visitor: JKVisitor) = visitor.visitJavaLabelSwitchCase(this)
}

class JKKtTryCatchSection(
    parameter: JKParameter,
    block: JKBlock
) : JKTreeElement() {
    var parameter: JKParameter by child(parameter)
    var block: JKBlock by child(block)
    override fun accept(visitor: JKVisitor) = visitor.visitKtTryCatchSection(this)
}

sealed class JKJavaResourceElement : JKTreeElement(), PsiOwner by PsiOwnerImpl()

class JKJavaResourceExpression(expression: JKExpression) : JKJavaResourceElement() {
    var expression by child(expression)
}
class JKJavaResourceDeclaration(declaration: JKLocalVariable) : JKJavaResourceElement() {
    var declaration by child(declaration)
}
