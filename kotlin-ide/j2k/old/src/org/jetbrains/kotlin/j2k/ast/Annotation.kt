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

import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append
import org.jetbrains.kotlin.j2k.buildList

enum class AnnotationUseTarget(val id: String) {
    File("file"), Param("param"), Get("get"), Set("set"), Field("field")
}

class Annotation(val name: Identifier,
                 val arguments: List<Pair<Identifier?, DeferredElement<Expression>>>,
                 val newLineAfter: Boolean,
                 val target: AnnotationUseTarget? = null) : Element() {

    override fun generateCode(builder: CodeBuilder) {
        builder.append("@")
        target?.let {
            builder.append("${it.id}:")
        }
        if (arguments.isEmpty()) {
            builder.append(name)
        }
        else {
            generateCall(builder)
        }
    }

    fun generateCall(builder: CodeBuilder) {
        builder.append(name)
                .append("(")
                .buildList(
                        generators = arguments.map {
                            {
                                if (it.first != null) {
                                    builder append it.first!! append " = " append it.second
                                }
                                else {
                                    builder append it.second
                                }
                            }
                        },
                        separator = ", ")
                .append(")")
    }

    override fun postGenerateCode(builder: CodeBuilder) {
        // we add line break in postGenerateCode to keep comments attached to this element on the same line
        builder.append(if (newLineAfter) "\n" else " ")
    }
}

class AnnotationConstructorCall(name: Identifier, arguments: List<Pair<Identifier?, DeferredElement<Expression>>>) : Expression() {
    private val annotation = Annotation(name, arguments, false)

    override fun generateCode(builder: CodeBuilder) {
        annotation.generateCall(builder)
    }
}

class Annotations(val annotations: List<Annotation>) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations, "")
    }

    override val isEmpty: Boolean
        get() = annotations.isEmpty()

    operator fun plus(other: Annotations) = Annotations(annotations + other.annotations).assignNoPrototype()

    companion object {
        val Empty = Annotations(listOf())
    }
}
