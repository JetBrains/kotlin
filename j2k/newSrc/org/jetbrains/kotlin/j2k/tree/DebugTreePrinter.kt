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

import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.utils.Printer

private class DebugTreePrinter : JKVisitorVoid {
    internal val stringBuilder = StringBuilder()
    private val printer = Printer(stringBuilder)

    override fun visitTreeElement(treeElement: JKTreeElement) {
        printer.println(treeElement.describe(), " [")
        printer.indented {
            treeElement.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) {
        printer.println(nameIdentifier.describe(), "(\"", nameIdentifier.value, "\")")
    }

    override fun visitJavaMethod(javaMethod: JKJavaMethod) {
        printer.print(javaMethod.modifiers().joinToString(" ") { it.text })
        printer.println(javaMethod.describe(), " [")
        printer.indented {
            javaMethod.block.accept(this, null)
            javaMethod.parameters.forEach { it.accept(this, null) }
        }
        printer.println("]")
    }

    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
        printer.println(expressionStatement.describe(), " [")
        printer.indented {
            expressionStatement.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
        printer.println(qualifiedExpression.describe(), " [")
        printer.indented {
            qualifiedExpression.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitBlock(block: JKBlock) {
        printer.println(block.describe(), " [")
        printer.indented {
            block.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitTypeElement(typeElement: JKTypeElement) {
        val type = typeElement.type
        printer.println(type.classNameWithoutJK(), " \"")
        printer.indented {
            if (type is JKClassType) {
                printer.println((type.classReference as? JKClassSymbol)?.fqName ?: type.classReference.let { it::class })
            }
            if (type is JKJavaPrimitiveType) {
                printer.println(type.jvmPrimitiveType.javaKeywordName)
            }
        }
        printer.println("\"")
    }

    override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
        printer.print(fieldAccessExpression.describe(), "(")
        printSymbol(fieldAccessExpression.identifier)
        printer.printlnWithNoIndent(")")
    }

    fun printSymbol(symbol: JKSymbol) {
        if (symbol is JKUniverseSymbol<*>) {
            printer.printWithNoIndent(symbol.target.describe())
        } else {
            printer.printWithNoIndent("Psi")
        }
    }
}

private fun JKTreeElement.describe(): String = this.classNameWithoutJK() + "@${this.hashCode().toString(16)}"
private fun JKTreeElement.classNameWithoutJK(): String = this.javaClass.simpleName.removePrefix("JK")
private fun JKType.classNameWithoutJK(): String = this.javaClass.simpleName.removePrefix("JK")

private inline fun Printer.indented(block: () -> Unit) {
    this.pushIndent()
    block()
    this.popIndent()
}


fun JKTreeElement.prettyDebugPrintTree(): String = DebugTreePrinter().apply { accept(this, null) }.stringBuilder.toString()