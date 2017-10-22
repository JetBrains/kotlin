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

    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier) {
        printer.println(javaAccessModifier.classNameWithoutJK(), "(", javaAccessModifier.type, ")")
    }
}

private fun Any.classNameWithoutJK(): String = this.javaClass.simpleName.removePrefix("JK")

private inline fun Printer.indented(block: () -> Unit) {
    this.pushIndent()
    block()
    this.popIndent()
}


fun JKElement.prettyDebugPrintTree(): String
        = DebugTreePrinter().apply { accept(this, null) }.stringBuilder.toString()