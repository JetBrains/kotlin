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

class Block(val statements: List<Statement>, val lBrace: LBrace, val rBrace: RBrace, val notEmpty: Boolean = false) : Statement() {
    override val isEmpty: Boolean
        get() = !notEmpty && statements.all { it.isEmpty }

    override fun generateCode(builder: CodeBuilder) {
        if (statements.all { it.isEmpty }) {
            if (!isEmpty) builder.append(lBrace).append(rBrace)
            return
        }

        builder.append(lBrace).append(statements, "\n", "\n", "\n").append(rBrace)
    }

    default object {
        val Empty = Block(listOf(), LBrace(), RBrace())
    }
}

// we use LBrace and RBrace elements to better handle comments around them
class LBrace() : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("{")
    }
}

class RBrace() : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("}")
    }
}
