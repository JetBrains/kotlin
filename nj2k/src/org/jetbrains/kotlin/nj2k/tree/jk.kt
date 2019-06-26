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

package org.jetbrains.kotlin.nj2k.tree

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor

interface JKTreeElement : JKElement, JKNonCodeElementsListOwner {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R

    fun <R> accept(visitor: JKVisitor<R, Nothing?>): R = accept(visitor, null)

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)

    fun acceptChildren(visitor: JKVisitor<Unit, Nothing?>) = acceptChildren(visitor, null)

    fun copy(): JKTreeElement
}

interface JKTreeRoot : JKTreeElement {
    var element: JKTreeElement
}

interface PsiOwner {
    var psi: PsiElement?
}

abstract class JKDeclaration : JKTreeElement, JKBranchElementBase() {
    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

interface JKImportStatement : JKTreeElement {
    val name: JKNameIdentifier
}

interface JKImportList : JKTreeElement {
    var imports: List<JKImportStatement>
}

interface JKFile : JKTreeElement, JKBranchElement {
    var packageDeclaration: JKPackageDeclaration
    var importList: JKImportList
    var declarationList: List<JKDeclaration>
}

abstract class JKClass : JKDeclaration(), JKVisibilityOwner, JKOtherModifiersOwner, JKModalityOwner, JKTypeParameterListOwner,
    JKAnnotationListOwner,
    JKBranchElement {
    abstract val name: JKNameIdentifier

    abstract val inheritance: JKInheritanceInfo

    abstract var classBody: JKClassBody
    abstract var classKind: ClassKind

    enum class ClassKind {
        ANNOTATION, CLASS, ENUM, INTERFACE, OBJECT, COMPANION
    }
}

fun JKClass.isLocalClass(): Boolean =
    parent !is JKClassBody && parent !is JKFile

val JKClass.declarationList: List<JKDeclaration>
    get() = classBody.declarations


interface JKInheritanceInfo : JKTreeElement, JKBranchElement {
    var extends: List<JKTypeElement>
    var implements: List<JKTypeElement>
}

fun JKInheritanceInfo.present(): Boolean =
    extends.isNotEmpty() || implements.isNotEmpty()

interface JKAnnotationList : JKTreeElement {
    var annotations: List<JKAnnotation>
}

interface JKAnnotation : JKAnnotationMemberValue {
    var classSymbol: JKClassSymbol
    var arguments: List<JKAnnotationParameter>
}

interface JKAnnotationParameter : JKTreeElement {
    var value: JKAnnotationMemberValue
}

interface JKAnnotationNameParameter : JKAnnotationParameter {
    val name: JKNameIdentifier
}

interface JKAnnotationListOwner : JKTreeElement {
    var annotationList: JKAnnotationList
}

abstract class JKMethod : JKDeclaration(), JKVisibilityOwner, JKModalityOwner, JKOtherModifiersOwner, JKTypeParameterListOwner,
    JKAnnotationListOwner {
    abstract val name: JKNameIdentifier
    abstract var parameters: List<JKParameter>
    abstract var returnType: JKTypeElement
    abstract var block: JKBlock

    val leftParen = JKTokenElementImpl("(")
    val rightParen = JKTokenElementImpl(")")
}

abstract class JKVariable : JKDeclaration(), JKAnnotationListOwner {
    abstract var type: JKTypeElement
    abstract var name: JKNameIdentifier
    abstract var initializer: JKExpression
}

abstract class JKForLoopVariable : JKVariable()

abstract class JKLocalVariable : JKVariable(), JKMutabilityOwner


abstract class JKModifierElement : JKTreeElement, JKBranchElementBase(), JKNonCodeElementsListOwner

val JKModifierElement.modifier: Modifier
    get() = when (this) {
        is JKMutabilityModifierElement -> mutability
        is JKModalityModifierElement -> modality
        is JKVisibilityModifierElement -> visibility
        is JKOtherModifierElement -> otherModifier
        else -> error("")
    }

abstract class JKMutabilityModifierElement : JKModifierElement() {
    abstract var mutability: Mutability
}

abstract class JKModalityModifierElement : JKModifierElement() {
    abstract var modality: Modality
}

abstract class JKVisibilityModifierElement : JKModifierElement() {
    abstract var visibility: Visibility
}

abstract class JKOtherModifierElement : JKModifierElement() {
    abstract var otherModifier: OtherModifier
}

interface Modifier {
    val text: String
}

interface JKOtherModifiersOwner : JKModifiersListOwner {
    var otherModifierElements: List<JKOtherModifierElement>
}

fun JKOtherModifiersOwner.elementByModifier(modifier: OtherModifier): JKOtherModifierElement? =
    otherModifierElements.firstOrNull { it.otherModifier == modifier }

fun JKOtherModifiersOwner.hasOtherModifier(modifier: OtherModifier): Boolean =
    otherModifierElements.any { it.otherModifier == modifier }

enum class OtherModifier(override val text: String) : Modifier {
    OVERRIDE("override"),
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

interface JKVisibilityOwner : JKModifiersListOwner {
    val visibilityElement: JKVisibilityModifierElement
}

enum class Visibility(override val text: String) : Modifier {
    PUBLIC("public"),
    INTERNAL("internal"),
    PROTECTED("protected"),
    PRIVATE("private")
}

var JKVisibilityOwner.visibility: Visibility
    get() = visibilityElement.visibility
    set(value) {
        visibilityElement.visibility = value
    }

interface JKModalityOwner : JKModifiersListOwner {
    val modalityElement: JKModalityModifierElement
}


enum class Modality(override val text: String) : Modifier {
    OPEN("open"),
    FINAL("final"),
    ABSTRACT("abstract"),
}

var JKModalityOwner.modality: Modality
    get() = modalityElement.modality
    set(value) {
        modalityElement.modality = value
    }

interface JKMutabilityOwner : JKModifiersListOwner {
    val mutabilityElement: JKMutabilityModifierElement
}

enum class Mutability(override val text: String) : Modifier {
    MUTABLE("var"),
    IMMUTABLE("val"),
    UNKNOWN("var")//TODO ???
}

var JKMutabilityOwner.mutability: Mutability
    get() = mutabilityElement.mutability
    set(value) {
        mutabilityElement.mutability = value
    }

interface JKModifiersListOwner : JKTreeElement

fun JKModifiersListOwner.modifierElements(): List<JKModifierElement> =
    listOfNotNull((this as? JKVisibilityOwner)?.visibilityElement) +
            (this as? JKOtherModifiersOwner)?.otherModifierElements.orEmpty() +
            listOfNotNull((this as? JKModalityOwner)?.modalityElement) +
            listOfNotNull((this as? JKMutabilityOwner)?.mutabilityElement)


interface JKTypeElement : JKTreeElement {
    val type: JKType
}

abstract class JKStatement : JKTreeElement, JKBranchElementBase() {
    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

abstract class JKBlock : JKTreeElement, JKBranchElementBase() {
    abstract var statements: List<JKStatement>

    val leftBrace = JKTokenElementImpl("{")
    val rightBrace = JKTokenElementImpl("}")
}

abstract class JKBodyStub : JKBlock() {

}

interface JKIdentifier : JKTreeElement

interface JKNameIdentifier : JKIdentifier {
    val value: String
}

interface JKExpression : JKTreeElement, JKAnnotationMemberValue

interface JKMethodReferenceExpression : JKExpression, PsiOwner {
    val qualifier: JKExpression
    val identifier: JKNamedSymbol
    val functionalType: JKTypeElement
    val isConstructorCall: Boolean
}

abstract class JKExpressionStatement : JKStatement() {
    abstract val expression: JKExpression
}

abstract class JKDeclarationStatement : JKStatement() {
    abstract val declaredStatements: List<JKDeclaration>
}

interface JKOperatorExpression : JKExpression {
    var operator: JKOperator
}

//TODO make left & right to be immutable
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
    var arguments: JKArgumentList
}

interface JKFieldAccessExpression : JKAssignableExpression {
    val identifier: JKFieldSymbol
}

interface JKPackageAccessExpression : JKAssignableExpression {
    val identifier: JKPackageSymbol
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

interface JKArgument : JKTreeElement, JKBranchElement {
    var value: JKExpression
}

interface JKNamedArgument : JKArgument {
    val name: JKNameIdentifier
}

interface JKArgumentList : JKTreeElement, JKBranchElement {
    var arguments: List<JKArgument>
}


interface JKLiteralExpression : JKExpression {
    val literal: String
    val type: LiteralType

    enum class LiteralType {
        STRING, CHAR, BOOLEAN, NULL, INT, LONG, FLOAT, DOUBLE
    }
}

abstract class JKParameter : JKVariable(), JKModifiersListOwner {
    abstract var isVarArgs: Boolean

    override var rightNonCodeElements: List<JKNonCodeElement> = emptyList()
}

interface JKStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKStubExpression : JKExpression

abstract class JKLoopStatement : JKStatement() {
    abstract var body: JKStatement
}

abstract class JKBlockStatement : JKStatement() {
    abstract var block: JKBlock
}

abstract class JKBlockStatementWithoutBrackets : JKStatement() {
    abstract var statements: List<JKStatement>
}

interface JKThisExpression : JKExpression {
    var qualifierLabel: JKLabel
}

interface JKSuperExpression : JKExpression {
    var qualifierLabel: JKLabel
}

abstract class JKWhileStatement : JKLoopStatement() {
    abstract var condition: JKExpression
}

abstract class JKDoWhileStatement : JKLoopStatement() {
    abstract var condition: JKExpression
}

abstract class JKBreakStatement : JKStatement()

abstract class JKBreakWithLabelStatement : JKBreakStatement() {
    abstract var label: JKNameIdentifier
}

abstract class JKIfStatement : JKStatement() {
    abstract var condition: JKExpression
    abstract var thenBranch: JKStatement
}

abstract class JKIfElseStatement : JKIfStatement() {
    abstract var elseBranch: JKStatement
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
    val functionalType: JKTypeElement
}

interface JKDelegationConstructorCall : JKMethodCallExpression {
    override val identifier: JKMethodSymbol
    val expression: JKExpression
    override var arguments: JKArgumentList
}

interface JKLabel : JKTreeElement

interface JKLabelEmpty : JKLabel

interface JKLabelText : JKLabel {
    val label: JKNameIdentifier
}

abstract class JKContinueStatement : JKStatement() {
    abstract var label: JKLabel
}

interface JKLabeledStatement : JKExpression {
    var statement: JKStatement
    val labels: List<JKNameIdentifier>
}

abstract class JKEmptyStatement : JKStatement()

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

abstract class JKEnumConstant : JKVariable() {
    abstract val arguments: JKArgumentList
    abstract val body: JKClassBody
}

abstract class JKForInStatement : JKStatement() {
    abstract var declaration: JKDeclaration
    abstract var iterationExpression: JKExpression
    abstract var body: JKStatement
}

abstract class JKPackageDeclaration : JKDeclaration() {
    abstract var packageName: JKNameIdentifier
}

interface JKClassLiteralExpression : JKExpression {
    val classType: JKTypeElement
    var literalType: LiteralType

    enum class LiteralType {
        KOTLIN_CLASS,
        JAVA_CLASS,
        JAVA_PRIMITIVE_CLASS,
        JAVA_VOID_TYPE
    }
}

interface JKAnnotationMemberValue : JKTreeElement