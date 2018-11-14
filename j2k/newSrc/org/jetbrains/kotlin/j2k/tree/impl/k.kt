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

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.JavaTokenType
import org.jetbrains.kotlin.j2k.ast.Nullability
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast

class JKKtPropertyImpl(
    modifierList: JKModifierList,
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    getter: JKBlock,
    setter: JKBlock
) : JKBranchElementBase(), JKKtProperty {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtProperty(this, data)

    override var modifierList: JKModifierList by child(modifierList)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)
    override var initializer: JKExpression by child(initializer)
    override val getter: JKBlock by child(getter)
    override val setter: JKBlock by child(setter)
}

class JKKtFunctionImpl(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    modifierList: JKModifierList,
    typeParameterList: JKTypeParameterList,
    annotationList: JKAnnotationList
) : JKBranchElementBase(), JKKtFunction {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtFunction(this, data)

    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var modifierList: JKModifierList by child(modifierList)
    override var typeParameterList: JKTypeParameterList by child(typeParameterList)
    override var annotationList: JKAnnotationList by child(annotationList)
}

sealed class JKKtQualifierImpl : JKQualifier, JKElementBase() {
    object DOT : JKKtQualifierImpl()
    object SAFE : JKKtQualifierImpl()
}

class JKKtCallExpressionImpl(
    override val identifier: JKMethodSymbol,
    arguments: JKExpressionList,
    typeArguments: List<JKTypeElement> = emptyList()
) : JKKtMethodCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtMethodCallExpression(this, data)

    override var arguments: JKExpressionList by child(arguments)
    override var typeArguments: List<JKTypeElement> by children(typeArguments)
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

class JKKtWordOperatorToken(override val text: String) : JKKtOperatorToken {
    override val operatorName: String = text
}

class JKKtOperatorImpl(override val token: JKKtOperatorToken, val methodSymbol: JKMethodSymbol) : JKOperator, JKElementBase() {
    constructor(singleValueToken: KtSingleValueToken, methodSymbol: JKMethodSymbol) : this(
        JKKtSingleValueOperatorToken(singleValueToken),
        methodSymbol
    )

    override val precedence: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


class JKKtModifierImpl(override val type: JKKtModifier.KtModifierType) : JKKtModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtModifier(this, data)
}

class JKKtAlsoCallExpressionImpl(
    statement: JKStatement, override val identifier: JKMethodSymbol, override val parameterName: String = "it"
) : JKKtAlsoCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtAlsoCallExpression(this, data)
    override var statement
        get() = arguments.expressions.first().cast<JKLambdaExpressionImpl>().statement
        set(it) {
            arguments.expressions.first().cast<JKLambdaExpressionImpl>().statement = it
        }
    val parameter
        get() = arguments.expressions.first().cast<JKLambdaExpression>().parameters.first()
    override var arguments: JKExpressionList by child(
        JKExpressionListImpl(
            listOf(
                JKLambdaExpressionImpl(
                    listOf(
                        JKParameterImpl(
                            JKTypeElementImpl(JKJavaVoidType),
                            JKNameIdentifierImpl(parameterName),
                            JKModifierListImpl()
                        )
                    ), statement
                )
            )
        )
    )
    override var typeArguments: List<JKTypeElement> by children(emptyList())
}

class JKKtAssignmentStatementImpl(
    override var field: JKAssignableExpression, expression: JKExpression, override var operator: JKOperator
) : JKKtAssignmentStatement, JKBranchElementBase() {
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtAssignmentStatement(this, data)

}

object JKContextType : JKType {
    override val nullability: Nullability
        get() = Nullability.Default
}

class JKKtConstructorImpl(
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    modifierList: JKModifierList,
    delegationCall: JKExpression
) : JKBranchElementBase(), JKKtConstructor {
    override val returnType: JKTypeElement get() = TODO("!")

    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var modifierList: JKModifierList by child(modifierList)
    override var delegationCall: JKExpression by child(delegationCall)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterListImpl())
    override var annotationList: JKAnnotationList by child(JKAnnotationListImpl())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtConstructor(this, data)
}

class JKKtPrimaryConstructorImpl(
    name: JKNameIdentifier,
    parameters: List<JKParameter>,
    block: JKBlock,
    modifierList: JKModifierList,
    delegationCall: JKExpression
) : JKBranchElementBase(), JKKtPrimaryConstructor {
    override val returnType: JKTypeElement get() = TODO("!")

    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)
    override var modifierList: JKModifierList by child(modifierList)
    override var delegationCall: JKExpression by child(delegationCall)
    override var typeParameterList: JKTypeParameterList by child(JKTypeParameterListImpl())
    override var annotationList: JKAnnotationList by child(JKAnnotationListImpl())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtPrimaryConstructor(this, data)
}

class JKKtWhenStatementImpl(
    expression: JKExpression,
    cases: List<JKKtWhenCase>
) : JKKtWhenStatement, JKBranchElementBase() {
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


class JKKtIsExpressionImpl(expression: JKExpression, type: JKTypeElement) : JKKtIsExpression, JKBranchElementBase() {
    override var type by child(type)
    override var expression by child(expression)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtIsExpression(this, data)
}

class JKKtInitDeclarationImpl(block: JKBlock) : JKKtInitDeclaration, JKBranchElementBase() {
    override var block: JKBlock by child(block)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtInitDeclaration(this, data)
}

fun JKClass.getOrCreateInitDeclaration(): JKKtInitDeclaration {
    val existingDeclaration = declarationList.filterIsInstance<JKKtInitDeclaration>().firstOrNull()
    if (existingDeclaration != null) return existingDeclaration
    val newDeclaration = JKKtInitDeclarationImpl(JKBlockImpl())
    declarationList += newDeclaration
    return newDeclaration
}

class JKKtOperatorExpressionImpl(
    receiver: JKExpression,
    override var identifier: JKMethodSymbol,
    argument: JKExpression
) : JKKtOperatorExpression, JKBranchElementBase() {
    override var receiver: JKExpression by child(receiver)
    override var argument: JKExpression by child(argument)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtOperatorExpression(this, data)
}

class JKKtConvertedFromForLoopSyntheticWhileStatementImpl(
    variableDeclaration: JKStatement,
    whileStatement: JKWhileStatement
) : JKKtConvertedFromForLoopSyntheticWhileStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
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
) : JKKtTryExpression, JKBranchElementBase(){
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