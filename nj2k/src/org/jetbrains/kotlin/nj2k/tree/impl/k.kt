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

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast

class JKKtPropertyImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    getter: JKKtGetterOrSetter,
    setter: JKKtGetterOrSetter,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement,
    mutabilityElement: JKMutabilityModifierElement
) : JKKtProperty(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtProperty(this, data)

    override var annotationList: JKAnnotationList by child(annotationList)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)
    override var initializer: JKExpression by child(initializer)
    override var getter: JKKtGetterOrSetter by child(getter)
    override var setter: JKKtGetterOrSetter by child(setter)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
    override var mutabilityElement by child(mutabilityElement)

    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

class JKKtFunctionImpl(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    typeParameterList: JKTypeParameterList,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKKtFunction(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtFunction(this, data)

    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var typeParameterList: JKTypeParameterList by child(typeParameterList)
    override var annotationList: JKAnnotationList by child(annotationList)


    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)
}

sealed class JKKtQualifierImpl : JKQualifier, JKElementBase() {
    object DOT : JKKtQualifierImpl()
    object SAFE : JKKtQualifierImpl()
}

class JKKtCallExpressionImpl(
    override val identifier: JKMethodSymbol,
    arguments: JKArgumentList,
    typeArgumentList: JKTypeArgumentList = JKTypeArgumentListImpl()
) : JKKtMethodCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtMethodCallExpression(this, data)

    override var arguments: JKArgumentList by child(arguments)
    override var typeArgumentList: JKTypeArgumentList by child(typeArgumentList)
}

class JKKtLiteralExpressionImpl(
    override val literal: String,
    override val type: JKLiteralExpression.LiteralType
) : JKKtLiteralExpression,
    JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtLiteralExpression(this, data)
}

class JKKtSingleValueOperatorToken(val psiToken: KtSingleValueToken) : JKKtOperatorToken {
    override val operatorName: String
        get() = OperatorConventions.getNameForOperationSymbol(psiToken, true, true)?.identifier
            ?: OperatorConventions.BOOLEAN_OPERATIONS[psiToken]?.identifier
            ?: TODO(psiToken.value)
    override val text: String = psiToken.value
}

object JKKtSpreadOperatorToken : JKKtOperatorToken {
    override val text: String = "*"
    override val operatorName: String = "*"
}

object JKKtSpreadOperator : JKOperator {
    override val token: JKOperatorToken = JKKtSpreadOperatorToken
    override val precedence: Int
        get() = TODO()
}

class JKKtWordOperatorToken(override val text: String) : JKKtOperatorToken {
    override val operatorName: String = text
}

class JKKtOperatorImpl(override val token: JKKtOperatorToken, val returnType: JKType) : JKOperator, JKElementBase() {
    constructor(singleValueToken: KtSingleValueToken, returnType: JKType) : this(
        JKKtSingleValueOperatorToken(singleValueToken),
        returnType
    )

    override val precedence: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


class JKKtAlsoCallExpressionImpl(
    statement: JKStatement,
    override val identifier: JKMethodSymbol,
    typeArgumentList: JKTypeArgumentList = JKTypeArgumentListImpl()
) : JKKtAlsoCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtAlsoCallExpression(this, data)
    override var statement
        get() = arguments.arguments.first().value.cast<JKLambdaExpressionImpl>().statement
        set(it) {
            arguments.arguments.first().value.cast<JKLambdaExpressionImpl>().statement = it
        }
    override var arguments: JKArgumentList by child(
        JKArgumentListImpl(
            JKLambdaExpressionImpl(statement, emptyList())
        )
    )
    override var typeArgumentList: JKTypeArgumentList by child(typeArgumentList)
}

class JKKtAssignmentStatementImpl(
    field: JKAssignableExpression,
    expression: JKExpression,
    override var operator: JKOperator
) : JKKtAssignmentStatement() {
    override var field: JKAssignableExpression by child(field)
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtAssignmentStatement(this, data)

    override var rightNonCodeElements: List<JKNonCodeElement> = listOf(JKSpaceElementImpl("\n"))
}

object JKContextType : JKType {
    override val nullability: Nullability
        get() = Nullability.Default
}

class JKKtConstructorImpl(
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    delegationCall: JKExpression,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKKtConstructor() {
    override var returnType: JKTypeElement by child(JKTypeElementImpl(JKNoTypeImpl))

    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var delegationCall: JKExpression by child(delegationCall)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterListImpl())
    override var annotationList: JKAnnotationList by child(annotationList)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtConstructor(this, data)
}

class JKKtPrimaryConstructorImpl(
    name: JKNameIdentifier,//TODO not needed
    parameters: List<JKParameter>,
    delegationCall: JKExpression,
    annotationList: JKAnnotationList,
    otherModifierElements: List<JKOtherModifierElement>,
    visibilityElement: JKVisibilityModifierElement,
    modalityElement: JKModalityModifierElement
) : JKKtPrimaryConstructor() {
    override var returnType: JKTypeElement by child(JKTypeElementImpl(JKNoTypeImpl))

    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(JKBodyStubImpl)
    override var delegationCall: JKExpression by child(delegationCall)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterListImpl())
    override var annotationList: JKAnnotationList by child(annotationList)

    override var otherModifierElements by children(otherModifierElements)
    override var visibilityElement by child(visibilityElement)
    override var modalityElement by child(modalityElement)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtPrimaryConstructor(this, data)
}

class JKKtWhenStatementImpl(
    expression: JKExpression,
    cases: List<JKKtWhenCase>
) : JKKtWhenStatement() {
    override var expression: JKExpression by child(expression)
    override var cases: List<JKKtWhenCase> by children(cases)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtWhenStatement(this, data)
}

class JKKtWhenCaseImpl(labels: List<JKKtWhenLabel>, statement: JKStatement) : JKKtWhenCase, JKBranchElementBase() {
    override var labels: List<JKKtWhenLabel> by children(labels)
    override var statement: JKStatement by child(statement)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtWhenCase(this, data)

}

class JKKtElseWhenLabelImpl : JKKtElseWhenLabel, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtElseWhenLabel(this, data)
}

class JKKtValueWhenLabelImpl(expression: JKExpression) : JKKtValueWhenLabel, JKBranchElementBase() {
    override var expression: JKExpression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtValueWhenLabel(this, data)
}


class JKKtIsExpressionImpl(expression: JKExpression, type: JKTypeElement) : JKKtIsExpression, JKBranchElementBase(),
    PsiOwner by PsiOwnerImpl() {
    override var type by child(type)
    override var expression by child(expression)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtIsExpression(this, data)
}

class JKKtInitDeclarationImpl(block: JKBlock) : JKKtInitDeclaration() {
    override var block: JKBlock by child(block)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtInitDeclaration(this, data)
}


class JKKtConvertedFromForLoopSyntheticWhileStatementImpl(
    variableDeclaration: JKStatement,
    whileStatement: JKWhileStatement
) : JKKtConvertedFromForLoopSyntheticWhileStatement(), PsiOwner by PsiOwnerImpl() {
    override var variableDeclaration: JKStatement by child(variableDeclaration)
    override var whileStatement: JKWhileStatement by child(whileStatement)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R =
        visitor.visitKtConvertedFromForLoopSyntheticWhileStatement(this, data)
}

class JKKtThrowExpressionImpl(exception: JKExpression) : JKKtThrowExpression, JKBranchElementBase() {
    override var exception: JKExpression by child(exception)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtThrowExpression(this, data)
}

class JKKtTryExpressionImpl(
    tryBlock: JKBlock,
    finallyBlock: JKBlock,
    catchSections: List<JKKtTryCatchSection>
) : JKKtTryExpression, JKBranchElementBase() {
    override var tryBlock: JKBlock by child(tryBlock)
    override var finallyBlock: JKBlock by child(finallyBlock)
    override var catchSections: List<JKKtTryCatchSection> by children(catchSections)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtTryExpression(this, data)
}

class JKKtTryCatchSectionImpl(
    parameter: JKParameter,
    block: JKBlock
) : JKKtTryCatchSection, JKBranchElementBase() {
    override var parameter: JKParameter by child(parameter)
    override var block: JKBlock by child(block)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtTryCatchSection(this, data)
}

class JKKtGetterOrSetterImpl(
    body: JKStatement,
    override val kind: JKKtGetterOrSetter.Kind,
    visibilityElement: JKVisibilityModifierElement
) : JKKtGetterOrSetter, JKBranchElementBase() {
    override var body: JKStatement by child(body)

    override var visibilityElement by child(visibilityElement)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtGetterOrSetter(this, data)
}

class JKKtEmptyGetterOrSetterImpl : JKKtEmptyGetterOrSetter, JKBranchElementBase() {
    override var body: JKStatement by child(JKEmptyStatementImpl())
    override val kind: JKKtGetterOrSetter.Kind
        get() = error("Cannot get kind of JKKtEmptyGetterOrSetter")

    override var visibilityElement by child(JKVisibilityModifierElementImpl(Visibility.PUBLIC))

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtEmptyGetterOrSetter(this, data)
}