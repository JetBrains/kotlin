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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.*

class Annotation(val name: Identifier, val arguments: List<Pair<Identifier?, DeferredElement<Expression>>>, val brackets: Boolean, val newLineAfter: Boolean) : Element() {
    private fun CodeBuilder.surroundWithBrackets(action: () -> Unit) {
        if (brackets) append("[")
        action()
        if (brackets) append("]")
    }

    override fun generateCode(builder: CodeBuilder) {
        if (arguments.isEmpty()) {
            builder.surroundWithBrackets { builder.append(name) }
        }
        else {
            builder.surroundWithBrackets {
                builder.append(name)
                        .append("(")
                        .append(arguments.map {
                            {
                                if (it.first != null) {
                                    builder append it.first!! append " = " append it.second
                                }
                                else {
                                    builder append it.second
                                }
                            }
                        }, ", ")
                        .append(")")
            }
        }
    }

    override fun postGenerateCode(builder: CodeBuilder) {
        // we add line break in postGenerateCode to keep comments attached to this element on the same line
        builder.append(if (newLineAfter) "\n" else " ")
    }
}

class Annotations(val annotations: List<Annotation>) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations, "")
    }

    override val isEmpty: Boolean
        get() = annotations.isEmpty()

    fun plus(other: Annotations) = Annotations(annotations + other.annotations).assignNoPrototype()

    default object {
        val Empty = Annotations(listOf())
    }
}

fun Annotations.withBrackets(): Annotations
        = Annotations(annotations.map { Annotation(it.name, it.arguments, true, it.newLineAfter).assignPrototypesFrom(it) }).assignPrototypesFrom(this)
