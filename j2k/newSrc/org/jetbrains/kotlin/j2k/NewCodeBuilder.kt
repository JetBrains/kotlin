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

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.j2k.NewCodeBuilder.ParenthesisKind.*
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class NewCodeBuilder {

    val builder = StringBuilder()
    val printer = Printer(builder)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
        JKClass.ClassKind.OBJECT -> "object"
        JKClass.ClassKind.COMPANION -> "companion object"
    }

    inner class Visitor : JKVisitorVoid {
        override fun visitTreeElement(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
        }

        override fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) {
            printer.printWithNoIndent("do")
            doWhileStatement.body.accept(this)
            printer.printWithNoIndent("while (")
            doWhileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
        }

        override fun visitFile(file: JKFile) {
            file.acceptChildren(this)
        }

        override fun visitBreakStatement(breakStatement: JKBreakStatement) {
            printer.printWithNoIndent("break")
        }

        override fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement) {
            printer.printWithNoIndent("break@")
            printer.printWithNoIndent(breakWithLabelStatement.label.value)
        }

        override fun visitModifierList(modifierList: JKModifierList) {
            modifierList.modifiers.firstOrNull()?.accept(this)
            for (i in 1..modifierList.modifiers.lastIndex) {
                printer.printWithNoIndent(" ")
                modifierList.modifiers[i].accept(this)
            }
        }

        override fun visitAccessModifier(accessModifier: JKAccessModifier) {
            printer.printWithNoIndent(
                when (accessModifier.visibility) {
                    JKAccessModifier.Visibility.PUBLIC -> "public"
                    JKAccessModifier.Visibility.PACKAGE_PRIVATE, JKAccessModifier.Visibility.INTERNAL -> "internal"
//                    JKAccessModifier.Visibility.PACKAGE_PRIVATE -> "internal /* package_local! */"
                    JKAccessModifier.Visibility.PROTECTED -> "protected"
                    JKAccessModifier.Visibility.PRIVATE -> "private"
                }
            )
        }

        override fun visitModalityModifier(modalityModifier: JKModalityModifier) {
            val containingDeclaration = modalityModifier.parent?.parent
            //TODO: merge with bottom one
            if (modalityModifier.modality == JKModalityModifier.Modality.ABSTRACT && containingDeclaration is JKClass) {
                if (containingDeclaration.classKind != JKClass.ClassKind.CLASS) return
            }

            printer.printWithNoIndent(
                when (modalityModifier.modality) {
                    JKModalityModifier.Modality.OPEN -> "open"
                    JKModalityModifier.Modality.FINAL -> "final"
                    JKModalityModifier.Modality.ABSTRACT -> "abstract"
                    JKModalityModifier.Modality.OVERRIDE -> "override"
                }
            )
        }

        override fun visitMutabilityModifier(mutabilityModifier: JKMutabilityModifier) {}

        override fun visitKtModifier(ktModifier: JKKtModifier) {
            printer.printWithNoIndent(
                when (ktModifier.type) {
                    JKKtModifier.KtModifierType.INTERNAL -> "internal"
                    JKKtModifier.KtModifierType.ABSTRACT -> "abstract"
                    JKKtModifier.KtModifierType.INNER -> "inner"
                    JKKtModifier.KtModifierType.OPEN -> "open"
                    JKKtModifier.KtModifierType.PRIVATE -> "private"
                    JKKtModifier.KtModifierType.PROTECTED -> "protected"
                    else -> ktModifier.type.toString()
                }
            )
        }


        private inline fun <T> renderList(list: List<T>, separator: String = ", ", renderElement: (T) -> Unit) {
            val (head, tail) = list.headTail()
            head?.let(renderElement) ?: return
            tail?.forEach {
                builder.append(separator)
                renderElement(it)
            }
        }

        override fun visitClass(klass: JKClass) {
            klass.modifierList.accept(this)
            builder.append(" ")
            printer.print(classKindString(klass.classKind))
            builder.append(" ")
            printer.printWithNoIndent(klass.name.value)

            val primaryConstructor = klass.primaryConstructor()
            if (primaryConstructor != null && primaryConstructor.parameters.isNotEmpty()) {
                renderParameterList(primaryConstructor.parameters)
            }

            if (klass.inheritance.inherit.isNotEmpty()) {
                printer.printWithNoIndent(" : ")

                val delegationCall = primaryConstructor?.delegationCall as? JKDelegationConstructorCall
                renderList(klass.inheritance.inherit) {
                    it.accept(this)
                    if (delegationCall != null && delegationCall.isCallOfConstructorOf(it.type)) {
                        printer.par {
                            delegationCall.arguments.accept(this)
                        }
                    }
                }
            }

            if (klass.declarationList.any { it !is JKKtPrimaryConstructor }) {
                printer.block(multiline = true) {
                    klass.declarationList.forEach { it.accept(this) }
                }
            } else {
                printer.println()
            }
        }

        override fun visitKtProperty(ktProperty: JKKtProperty) {
            ktProperty.modifierList.accept(this)

            when (ktProperty.modifierList.mutability) {
                Mutability.Default, Mutability.Mutable -> printer.print("var")
                Mutability.NonMutable -> printer.print("val")
            }

            printer.printWithNoIndent(" ", ktProperty.name.value, ": ")
            ktProperty.type.accept(this)
            if (ktProperty.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                ktProperty.initializer.accept(this)
            }
            printer.printlnWithNoIndent()
        }

        override fun visitKtIsExpression(ktIsExpression: JKKtIsExpression) {
            ktIsExpression.expression.accept(this)
            printer.printWithNoIndent(" is ")
            ktIsExpression.type.accept(this)
        }


        override fun visitParameter(parameter: JKParameter) {
            printer.printWithNoIndent(parameter.name.value, ": ")
            parameter.type.accept(this)
            if (parameter.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                parameter.initializer.accept(this)
            }
        }

        override fun visitKtFunction(ktFunction: JKKtFunction) {
            printer.printIndent()
            ktFunction.modifierList.accept(this)
            printer.printWithNoIndent(" fun ", ktFunction.name.value, "(")
            renderList(ktFunction.parameters) {
                it.accept(this)
            }
            printer.printWithNoIndent(")", ": ")
            ktFunction.returnType.accept(this)
            if (ktFunction.block !== JKBodyStub) {
                printer.block(multiline = ktFunction.block.statements.isNotEmpty()) {
                    ktFunction.block.accept(this)
                }
            }
        }


        override fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) {
            printer.printWithNoIndent("if (")
            ifElseExpression.condition.accept(this)
            printer.printWithNoIndent(")")
            ifElseExpression.thenBranch.accept(this)
            printer.printWithNoIndent(" else ")
            ifElseExpression.elseBranch.accept(this)
        }

        override fun visitIfStatement(ifStatement: JKIfStatement) {
            printer.printWithNoIndent("if (")
            ifStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            renderStatementOrBlock(ifStatement.thenBranch)
        }

        override fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) {
            visitIfStatement(ifElseStatement)
            printer.printWithNoIndent(" else ")
            renderStatementOrBlock(ifElseStatement.elseBranch)
        }

        private fun renderStatementOrBlock(statement: JKStatement, multiline: Boolean = false) {
            if (statement is JKBlockStatement) {
                printer.block(multiline) {
                    statement.block.statements.forEach { it.accept(this) }
                }
            } else {
                statement.accept(this)
            }
        }

        override fun visitBinaryExpression(binaryExpression: JKBinaryExpression) {
            binaryExpression.left.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(binaryExpression.operator.operatorText)
            printer.printWithNoIndent(" ")
            binaryExpression.right.accept(this)
        }

        override fun visitLiteralExpression(literalExpression: JKLiteralExpression) {
            printer.printWithNoIndent(literalExpression.literal)
        }

        override fun visitPrefixExpression(prefixExpression: JKPrefixExpression) {
            printer.printWithNoIndent(prefixExpression.operator.operatorText)
            prefixExpression.expression.accept(this)
        }

        override fun visitThisExpression(thisExpression: JKThisExpression) {
            printer.printWithNoIndent("this")
        }

        override fun visitSuperExpression(superExpression: JKSuperExpression) {
            printer.printWithNoIndent("super")
        }

        override fun visitPostfixExpression(postfixExpression: JKPostfixExpression) {
            postfixExpression.expression.accept(this)
            printer.printWithNoIndent(postfixExpression.operator.operatorText)
        }

        override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
            qualifiedExpression.receiver.accept(this)
            printer.printWithNoIndent(
                when (qualifiedExpression.operator) {
                    is JKJavaQualifierImpl.DOT /*<-remove this TODO!*/, is JKKtQualifierImpl.DOT -> "."
                    is JKKtQualifierImpl.SAFE -> "?."
                    else -> TODO()
                }
            )
            qualifiedExpression.selector.accept(this)
        }

        override fun visitExpressionList(expressionList: JKExpressionList) {
            renderList(expressionList.expressions) { it.accept(this) }
        }

        override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) {
            printer.printWithNoIndent(FqName(methodCallExpression.identifier.fqName).shortName().asString())
            if (methodCallExpression.typeArguments.isNotEmpty()) {
                printer.par(ANGLE) {
                    renderList(methodCallExpression.typeArguments) { it.accept(this) }
                }
            }
            printer.par {
                methodCallExpression.arguments.accept(this)
            }
        }

        override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) {
            printer.par {
                parenthesizedExpression.expression.accept(this)
            }
        }

        override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) {
            declarationStatement.declaredStatements.forEach {
                it.accept(this)
            }
        }

        override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) {
            typeCastExpression.expression.accept(this)
            printer.printWithNoIndent(" as ")
            typeCastExpression.type.accept(this)
        }

        override fun visitWhileStatement(whileStatement: JKWhileStatement) {
            printer.print("while(")
            whileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            renderStatementOrBlock(whileStatement.body, multiline = true)
        }

        override fun visitLocalVariable(localVariable: JKLocalVariable) {
            if (localVariable.modifierList.modality == JKModalityModifier.Modality.FINAL) {
                printer.print("val")
            } else {
                printer.print("var")
            }

            printer.printWithNoIndent(" ", localVariable.name.value)
            if (localVariable.type.type != JKContextType) {
                printer.printWithNoIndent(": ")
                localVariable.type.accept(this)
            }
            if (localVariable.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                localVariable.initializer.accept(this)
            }
            printer.printlnWithNoIndent()
        }


        private fun renderType(type: JKType) {
            when (type) {
                is JKClassType -> type.classReference.fqName?.let { printer.printWithNoIndent(FqName(it).shortName().asString()) }
                is JKUnresolvedClassType -> printer.printWithNoIndent(type.name)
                is JKContextType -> return
                is JKStarProjectionType ->
                    printer.printWithNoIndent("*")
                else -> printer.printWithNoIndent("Unit /* TODO: ${type::class} */")
            }
            if (type is JKParametrizedType && type.parameters.isNotEmpty()) {
                printer.par(ANGLE) {
                    renderList(type.parameters, renderElement = ::renderType)
                }
            }
            when (type.nullability) {
                Nullability.Nullable -> printer.printWithNoIndent("?")
                Nullability.Default -> printer.printWithNoIndent("?")// /* TODO: Default */")
                else -> {
                }
            }
        }

        override fun visitTypeElement(typeElement: JKTypeElement) {
            renderType(typeElement.type)
        }

        override fun visitBlock(block: JKBlock) {
            block.acceptChildren(this)
        }

        override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
            printer.printIndent()
            expressionStatement.expression.accept(this)
            printer.printlnWithNoIndent()
        }

        override fun visitReturnStatement(returnStatement: JKReturnStatement) {
            printer.print("return ")
            returnStatement.expression.accept(this)
            printer.printlnWithNoIndent()
        }

        override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
            printer.printWithNoIndent(FqName(fieldAccessExpression.identifier.fqName).shortName().asString())
        }

        override fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression) {
            arrayAccessExpression.expression.accept(this)
            printer.par(SQUARE) { arrayAccessExpression.indexExpression.accept(this) }
        }

        override fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) {
            delegationConstructorCall.expression.accept(this)
            printer.par {
                delegationConstructorCall.arguments.accept(this)
            }
        }

        private fun renderParameterList(parameters: List<JKParameter>) {
            printer.par(ROUND) {
                renderList(parameters) {
                    it.accept(this)
                }
            }
        }

        override fun visitKtConstructor(ktConstructor: JKKtConstructor) {
            printer.print("constructor")
            renderParameterList(ktConstructor.parameters)
            if (ktConstructor.delegationCall !is JKStubExpression) {
                builder.append(" : ")
                ktConstructor.delegationCall.accept(this)
            }
            if (ktConstructor.block !== JKBodyStub) {
                printer.block(multiline = true) {
                    ktConstructor.block.accept(this)
                }
            }
        }

        override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) {}

        private inline fun Printer.indented(block: () -> Unit) {
            this.pushIndent()
            block()
            this.popIndent()
        }

        private inline fun Printer.block(multiline: Boolean = false, crossinline body: () -> Unit) {
            par(if (multiline) CURVED_MULTILINE else CURVED) {
                indented(body)
            }
        }

        private inline fun Printer.par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
            this.printWithNoIndent(kind.open)
            body()
            this.printWithNoIndent(kind.close)
        }

        override fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) {
            printer.par(CURVED_MULTILINE) {
                lambdaExpression.parameters.firstOrNull()?.accept(this)
                lambdaExpression.parameters.asSequence().drop(1).forEach { printer.printWithNoIndent(", "); it.accept(this) }
                if (lambdaExpression.parameters.isNotEmpty()) {
                    printer.printWithNoIndent(" -> ")
                }

                val statement = lambdaExpression.statement
                if (statement is JKBlockStatement) {
                    statement.block.statements.forEach { it.accept(this) }
                } else {
                    statement.accept(this)
                }
            }
        }

        override fun visitBlockStatement(blockStatement: JKBlockStatement) {
            printer.par(CURVED) {
                blockStatement.block.accept(this)
            }
        }

        /*override fun visitParameter(parameter: JKParameter) {
            printer.printWithNoIndent(parameter.name.value)
        }*/

        override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) {
            ktAssignmentStatement.field.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(ktAssignmentStatement.operator.operatorText)
            printer.printWithNoIndent(" ")
            ktAssignmentStatement.expression.accept(this)
        }

        override fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) {
            printer.printWithNoIndent("when(")
            ktWhenStatement.expression.accept(this)
            printer.printWithNoIndent(")")
            printer.block(multiline = true) {
                ktWhenStatement.cases.forEach { it.accept(this) }
            }
        }

        override fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) {
            renderList(ktWhenCase.labels, ", ") {
                it.accept(this)
            }
            printer.printWithNoIndent(" -> ")
            ktWhenCase.statement.accept(this)
        }

        override fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) {
            printer.printWithNoIndent("else")
        }

        override fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) {
            ktValueWhenLabel.expression.accept(this)
        }
    }

    private enum class ParenthesisKind(val open: String, val close: String) {
        ROUND("(", ")"), SQUARE("[", "]"), CURVED("{", "}"), CURVED_MULTILINE("{\n", "}\n"), INLINE_COMMENT("/*", "*/"), ANGLE("<", ">")
    }

    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return builder.toString()
    }
}



private inline fun <T> List<T>.headTail(): Pair<T?, List<T>?> {
    val head = this.firstOrNull()
    val tail = if (size <= 1) null else subList(1, size)
    return head to tail
}

private inline fun JKClass.primaryConstructor(): JKKtPrimaryConstructor? {
    return this.declarationList.firstIsInstanceOrNull()
}

private inline fun JKDelegationConstructorCall.isCallOfConstructorOf(type: JKType): Boolean {
    return when (type) {
        is JKClassType -> {
            val symbol = type.classReference as? JKClassSymbol ?: return false
            this.identifier.name == symbol.name && this.identifier.declaredIn == symbol
        }
        is JKUnresolvedClassType -> {
            this.identifier.name == type.name
        }
        else -> false
    }
}