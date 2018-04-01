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

import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.utils.Printer

private class DebugTreePrinter : JKVisitorVoid {
    internal val stringBuilder = StringBuilder()
    private val printer = Printer(stringBuilder)

    override fun visitElement(element: JKElement) {
        printer.println(element.classNameWithoutJK(), " [")
        printer.indented {
            element.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitJavaModifier(javaModifier: JKJavaModifier) {
        printer.println(javaModifier.classNameWithoutJK(), "(", javaModifier.type, ")")
    }

    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier) {
        printer.println(javaAccessModifier.classNameWithoutJK(), "(", javaAccessModifier.type, ")")
    }

    override fun visitJavaMethod(javaMethod: JKJavaMethod) {
        printer.println(javaMethod.classNameWithoutJK(), " [")
        printer.indented {
            javaMethod.block?.accept(this, null)
            javaMethod.modifierList.accept(this, null)
            javaMethod.valueArguments.forEach { it.accept(this, null) }
        }
        printer.println("]")
    }

    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
        printer.println(expressionStatement.classNameWithoutJK(), " [")
        printer.indented {
            expressionStatement.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
        printer.println(qualifiedExpression.classNameWithoutJK(), " [")
        printer.indented {
            qualifiedExpression.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitBlock(block: JKBlock) {
        printer.println(block.classNameWithoutJK(), " [")
        printer.indented {
            block.acceptChildren(this, null)
        }
        printer.println("]")
    }

    override fun visitMethodReference(methodReference: JKMethodReference) {
        printer.println(methodReference.classNameWithoutJK())
        printer.println(methodReference.target.parent.let { (it as? JKClass)?.name?.name ?: "null" }, "@", methodReference.target.name.name)
    }

    override fun visitFieldReference(fieldReference: JKFieldReference) {
        printer.println(fieldReference.classNameWithoutJK())
        printer.println(fieldReference.target.parent.let { (it as? JKClass)?.name?.name ?: "null" }, "@", fieldReference.target.name.name)
    }

    override fun visitClassReference(classReference: JKClassReference) {
        printer.println(classReference.classNameWithoutJK())
        printer.println(classReference.target.name.name)
    }

    override fun visitType(type: JKType) {
        printer.println(type.classNameWithoutJK(), " \"")
        printer.indented {
            if (type is JKClassType)
                printer.println(type.classReference.target.name.name)
        }
        printer.println("\"")
    }
}

private fun Any.classNameWithoutJK(): String = this.javaClass.simpleName.removePrefix("JK")

private inline fun Printer.indented(block: () -> Unit) {
    this.pushIndent()
    block()
    this.popIndent()
}


fun JKElement.prettyDebugPrintTree(): String = DebugTreePrinter().apply { accept(this, null) }.stringBuilder.toString()