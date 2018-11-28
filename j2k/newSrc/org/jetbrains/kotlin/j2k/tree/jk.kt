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

package org.jetbrains.kotlin.j2k.tree

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKMethodSymbol
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

interface JKTreeElement : JKElement {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R

    fun <R> accept(visitor: JKVisitor<R, Nothing?>): R = accept(visitor, null)

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)

    fun acceptChildren(visitor: JKVisitor<Unit, Nothing?>) = acceptChildren(visitor, null)

    fun copy(): JKTreeElement
}

interface PsiOwner {
    var psi: PsiElement?
}

interface JKDeclaration : JKTreeElement

interface JKImportStatement : JKTreeElement {
    val name: JKNameIdentifier
}

interface JKFile : JKTreeElement, JKBranchElement {
    var packageDeclaration: JKPackageDeclaration
    var importList: List<JKImportStatement>
    var declarationList: List<JKDeclaration>
}

interface JKClass : JKDeclaration, JKVisibilityOwner, JKExtraModifiersOwner, JKModalityOwner, JKTypeParameterListOwner, JKBranchElement {
    val name: JKNameIdentifier

    val inheritance: JKInheritanceInfo

    var classBody: JKClassBody
    var classKind: ClassKind

    enum class ClassKind {
        ANNOTATION, CLASS, ENUM, INTERFACE, OBJECT, COMPANION
    }
}

val JKClass.declarationList: List<JKDeclaration>
    get() = classBody.declarations


interface JKInheritanceInfo : JKTreeElement, JKBranchElement {
    val inherit: List<JKTypeElement>
}

interface JKAnnotationList : JKTreeElement {
    var annotations: List<JKAnnotation>
}

interface JKAnnotation : JKTreeElement {
    val classSymbol: JKClassSymbol
    var arguments: JKExpressionList
}

interface JKAnnotationListOwner : JKTreeElement {
    var annotationList: JKAnnotationList
}

interface JKMethod : JKDeclaration, JKVisibilityOwner, JKModalityOwner, JKExtraModifiersOwner, JKTypeParameterListOwner,
    JKAnnotationListOwner {
    val name: JKNameIdentifier
    var parameters: List<JKParameter>
    val returnType: JKTypeElement
    var block: JKBlock
}

interface JKVariable : JKDeclaration {
    var type: JKTypeElement
    var name: JKNameIdentifier
    var initializer: JKExpression
}

interface JKForLoopVariable : JKVariable

interface JKLocalVariable : JKVariable, JKMutabilityOwner

interface JKExtraModifiersOwner : JKModifiersListOwner {
    var extraModifiers: List<ExtraModifier>
}

interface Modifier {
    val text: String
}

enum class ExtraModifier(override val text: String) : Modifier {
    ACTUAL("actual"),
    ANNOTATION("annotation"),
    COMPANION("companion"),
    CONST("const"),
    CROSSINLINE("crossinline"),
    DATA("data"),
    EXPECT("expect"),
    EXTERNAL("external"),
    INFIX("infix"),
    INLINE("inline"),
    INNER("inner"),
    LATEINIT("lateinit"),
    NOINLINE("noinline"),
    OPERATOR("operator"),
    OUT("out"),
    REIFIED("reified"),
    SEALED("sealed"),
    SUSPEND("suspend"),
    TAILREC("tailrec"),
    VARARG("vararg"),

    NATIVE("native"),
    STATIC("static"),
    STRICTFP("strictfp"),
    SYNCHRONIZED("synchronized"),
    TRANSIENT("transient"),
    VOLATILE("volatile")
}

interface JKVisibilityOwner : JKModifiersListOwner{
    var visibility: Visibility
}

enum class Visibility(override val text: String) : Modifier {
    PUBLIC("public"),
    INTERNAL("internal"),
    PACKAGE_PRIVATE(""),
    PROTECTED("protected"),
    PRIVATE("private")
}

interface JKModalityOwner : JKModifiersListOwner {
    var modality: Modality
}


enum class Modality(override val text: String) : Modifier {
    OPEN("open"),
    FINAL("final"),
    ABSTRACT("abstract"),
    OVERRIDE("override")
}

interface JKMutabilityOwner : JKModifiersListOwner {
    var mutability: Mutability
}

enum class Mutability(override val text: String) : Modifier {
    MUTABLE("var"),
    IMMUTABLE("val"),
    UNKNOWN("var")//TODO ???
}

interface JKModifiersListOwner : JKTreeElement

fun JKModifiersListOwner.modifiers(): List<Modifier> =
    listOfNotNull((this as? JKVisibilityOwner)?.visibility) +
            (this as? JKExtraModifiersOwner)?.extraModifiers.orEmpty() +
            listOfNotNull((this as? JKModalityOwner)?.modality) +
            listOfNotNull((this as? JKMutabilityOwner)?.mutability)


interface JKTypeElement : JKTreeElement {
    val type: JKType
}

interface JKStatement : JKTreeElement

interface JKBlock : JKTreeElement {
    var statements: List<JKStatement>
}

interface JKIdentifier : JKTreeElement

interface JKNameIdentifier : JKIdentifier {
    val value: String
}

interface JKExpression : JKTreeElement

interface JKExpressionStatement : JKStatement, JKBranchElement {
    val expression: JKExpression
}

interface JKDeclarationStatement : JKStatement {
    val declaredStatements: List<JKDeclaration>
}

interface JKOperatorExpression : JKExpression {
    var operator: JKOperator
}

interface JKBinaryExpression : JKOperatorExpression {
    var left: JKExpression
    var right: JKExpression
}

interface JKUnaryExpression : JKOperatorExpression {
    var expression: JKExpression
}

interface JKPrefixExpression : JKUnaryExpression

interface JKPostfixExpression : JKUnaryExpression

interface JKQualifiedExpression : JKExpression, JKAssignableExpression {
    var receiver: JKExpression
    var operator: JKQualifier
    var selector: JKExpression
}

interface JKTypeArgumentList : JKTreeElement {
    val typeArguments: List<JKTypeElement>
}

interface JKTypeArgumentListOwner : JKTreeElement {
    var typeArgumentList: JKTypeArgumentList
}

interface JKMethodCallExpression : JKExpression, JKTypeArgumentListOwner, JKBranchElement {
    val identifier: JKMethodSymbol
    val arguments: JKExpressionList
}

interface JKFieldAccessExpression : JKAssignableExpression {
    val identifier: JKFieldSymbol
}

interface JKClassAccessExpression : JKExpression {
    val identifier: JKClassSymbol
}

interface JKArrayAccessExpression : JKAssignableExpression {
    var expression: JKExpression
    var indexExpression: JKExpression
}

interface JKParenthesizedExpression : JKExpression {
    val expression: JKExpression
}

interface JKTypeCastExpression : JKExpression {
    val expression: JKExpression
    val type: JKTypeElement
}

interface JKExpressionList : JKTreeElement, JKBranchElement {
    var expressions: List<JKExpression>
}

interface JKLiteralExpression : JKExpression {
    val literal: String
    val type: LiteralType

    enum class LiteralType {
        STRING, CHAR, BOOLEAN, NULL, INT, LONG, FLOAT, DOUBLE
    }
}

interface JKParameter : JKVariable, JKModifiersListOwner

interface JKStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKStubExpression : JKExpression

interface JKLoopStatement : JKStatement {
    var body: JKStatement
}

interface JKBlockStatement : JKStatement, JKBranchElement {
    var block: JKBlock
}

interface JKThisExpression : JKExpression {
    var qualifierLabel: JKLabel
}

interface JKSuperExpression : JKExpression

interface JKWhileStatement : JKLoopStatement {
    var condition: JKExpression
}

interface JKDoWhileStatement : JKLoopStatement {
    var condition: JKExpression
}

interface JKBreakStatement : JKStatement

interface JKBreakWithLabelStatement : JKBreakStatement {
    var label: JKNameIdentifier
}

interface JKIfStatement : JKStatement {
    var condition: JKExpression
    var thenBranch: JKStatement
}

interface JKIfElseStatement : JKIfStatement {
    var elseBranch: JKStatement
}

interface JKIfElseExpression : JKExpression {
    var condition: JKExpression
    var thenBranch: JKExpression
    var elseBranch: JKExpression
}

interface JKAssignableExpression : JKExpression

interface JKLambdaExpression : JKExpression {
    var parameters: List<JKParameter>
    val returnType: JKTypeElement
    var statement: JKStatement
}

interface JKDelegationConstructorCall : JKMethodCallExpression {
    override val identifier: JKMethodSymbol
    val expression: JKExpression
    override val arguments: JKExpressionList
}

interface JKLabel : JKTreeElement

interface JKLabelEmpty : JKLabel

interface JKLabelText : JKLabel {
    val label: JKNameIdentifier
}

interface JKContinueStatement : JKStatement {
    var label: JKLabel
}

interface JKLabeledStatement : JKStatement {
    var statement: JKStatement
    val labels: List<JKNameIdentifier>
}

interface JKEmptyStatement : JKStatement

interface JKTypeParameterList : JKTreeElement {
    var typeParameters: List<JKTypeParameter>
}

interface JKTypeParameter : JKTreeElement {
    var name: JKNameIdentifier
    var upperBounds: List<JKTypeElement>
}

interface JKTypeParameterListOwner : JKTreeElement {
    var typeParameterList: JKTypeParameterList
}

interface JKEnumConstant : JKVariable {
    val arguments: JKExpressionList
}

interface JKForInStatement : JKStatement {
    var declaration: JKDeclaration
    var iterationExpression: JKExpression
    var body: JKStatement
}

interface JKPackageDeclaration : JKDeclaration {
    var packageName: JKNameIdentifier
}

interface JKClassLiteralExpression : JKExpression {
    val classType: JKTypeElement
    val literalType: LiteralType

    enum class LiteralType {
        KOTLIN_CLASS,
        JAVA_CLASS,
        JAVA_PRIMITIVE_CLASS
    }
}