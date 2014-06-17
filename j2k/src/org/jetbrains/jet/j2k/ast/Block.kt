/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import java.util.ArrayList
import org.jetbrains.jet.j2k.CommentConverter

fun Block(statements: List<Statement>, notEmpty: Boolean = false): Block {
    val elements = ArrayList<Element>()
    elements.add(WhiteSpace.NewLine)
    elements.addAll(statements)
    elements.add(WhiteSpace.NewLine)
    return Block(StatementList(elements), notEmpty)
}

class Block(val statementList: StatementList, val notEmpty: Boolean = false) : Statement() {
    val statements: List<Statement> = statementList.statements


    override val isEmpty: Boolean
        get() = !notEmpty && statements.all { it.isEmpty }

    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        if (!isEmpty) {
            return "{${statementList.toKotlin(commentConverter)}}"
        }

        return ""
    }

    class object {
        val Empty = Block(StatementList(listOf()))
    }
}
