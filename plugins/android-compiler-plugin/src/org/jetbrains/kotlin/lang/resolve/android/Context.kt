/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.lang.resolve.android

import java.util.ArrayList


open class Context(val buffer: StringBuffer = StringBuffer(), private var indentDepth: Int = 0) {
    open class InvalidIndent(num: Int) : RuntimeException("Indentation level < 0: $num")

    val indentUnit = "    "
    protected var currentIndent: String = indentUnit.repeat(indentDepth)
    private val children = ArrayList<Context>()

    public fun incIndent() {
        indentDepth++
        currentIndent += indentUnit
    }

    public fun decIndent() {
        indentDepth--
        if (indentDepth < 0)
            throw InvalidIndent(indentDepth)
        currentIndent = currentIndent.substring(0, currentIndent.length - indentUnit.length)
    }

    public open fun write(what: String) {
        writeNoIndent(currentIndent)
        writeNoIndent(what)
    }

    public fun writeNoIndent(what: String) {
        buffer.append(what)
    }

    public fun writeln(what: String) {
        write(what)
        newLine()
    }

    public fun newLine() {
        writeNoIndent("\n")
    }


    public fun trim(num: Int) {
        buffer.delete(buffer.length() - num, buffer.length())
    }

    public fun fork(newBuffer: StringBuffer = StringBuffer(),
                    newIndentDepth: Int = indentDepth): Context {
        val child = Context(newBuffer, newIndentDepth)
        children.add(child)
        return child
    }

    public fun adopt<T : Context>(c: T, inheritIndent: Boolean = true): T {
        children.add(c)
        if (inheritIndent) c.currentIndent = currentIndent
        return c
    }

    public fun absorbChildren(noIndent: Boolean = true) {
        for (child in children) {
            child.absorbChildren()
            if (noIndent)
                writeNoIndent(child.toString())
            else
                write(child.toString())
        }
        children.clear()
    }

    public override fun toString(): String {
        return buffer.toString()
    }
}

