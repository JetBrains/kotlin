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

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer

class NewCodeBuilder {

    val builder = StringBuilder()
    val printer = Printer(builder)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ABSTRACT -> "abstract class"
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
    }

    inner class Visitor : JKVisitorVoid {
        override fun visitTreeElement(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
        }

        override fun visitFile(file: JKFile) {
            file.acceptChildren(this)
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
                }
            )
        }

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

        override fun visitClass(klass: JKClass) {
            klass.modifierList.accept(this)
            builder.append(" ")
            printer.print(classKindString(klass.classKind))
            builder.append(" ")
            printer.printWithNoIndent(klass.name.value)
            if (klass.declarationList.isNotEmpty()) {
                printer.println(" {")
                printer.pushIndent()
                klass.declarationList.forEach { it.accept(this) }
                printer.popIndent()
                printer.println("}")

            } else {
                printer.println()
            }
        }

        override fun visitKtProperty(ktProperty: JKKtProperty) {
            // TODO: Fix this, as Modality is not mutability
            if (ktProperty.modifierList.modality == JKModalityModifier.Modality.FINAL) {
                printer.print("val")
            } else {
                printer.print("var")
            }

            printer.printWithNoIndent(" ", ktProperty.name.value)
            ktProperty.type.accept(this)
            if (ktProperty.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                ktProperty.initializer.accept(this)
            }
            printer.printlnWithNoIndent()
        }

        override fun visitKtFunction(ktFunction: JKKtFunction) {
            printer.print("fun ", ktFunction.name.value, "(", ")")
            ktFunction.returnType.accept(this)
            if (ktFunction.block !== JKBodyStub) {
                printer.printlnWithNoIndent(" {")
                printer.pushIndent()
                ktFunction.block.accept(this)
                printer.popIndent()
                printer.printWithNoIndent("}")
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
            expressionList.expressions.firstOrNull()?.accept(this)
            for (i in 1..expressionList.expressions.lastIndex) {
                printer.printWithNoIndent(", ")
                expressionList.expressions[i].accept(this)
            }
        }

        override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) {
            printer.printWithNoIndent(FqName(methodCallExpression.identifier.fqName).shortName().asString())
            printer.printWithNoIndent("(")
            methodCallExpression.arguments.accept(this)
            printer.printWithNoIndent(")")
        }

        override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) {
            printer.printWithNoIndent("(")
            parenthesizedExpression.expression.accept(this)
            printer.printWithNoIndent(")")
        }

        override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) {
            declarationStatement.declaredStatements.forEach {
                it.accept(this)
            }
        }

        override fun visitLocalVariable(localVariable: JKLocalVariable) {
            if (localVariable.modifierList.modality == JKModalityModifier.Modality.FINAL) {
                printer.print("val")
            } else {
                printer.print("var")
            }

            printer.printWithNoIndent(" ", localVariable.name.value)
            localVariable.type.accept(this)
            if (localVariable.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                localVariable.initializer.accept(this)
            }
            printer.printlnWithNoIndent()
        }

        override fun visitTypeElement(typeElement: JKTypeElement) {
            val type = typeElement.type
            when (type) {
                is JKClassType -> if ((type.classReference as? JKClassSymbol)?.fqName != "kotlin.Unit") {
                    (type.classReference as JKClassSymbol).fqName?.let { printer.printWithNoIndent(":" + FqName(it).shortName().asString()) }
                }
                else -> printer.printWithNoIndent(":Unit /* TODO: ${type::class} */")
            }
            when (type.nullability) {
                Nullability.Nullable -> printer.printWithNoIndent("?")
                Nullability.Default -> printer.printWithNoIndent("?")// /* TODO: Default */")
                else -> {
                }
            }
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
            printer.printWithNoIndent("[")
            arrayAccessExpression.indexExpression.accept(this)
            printer.printWithNoIndent("]")
        }
    }

    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return builder.toString()
    }
}