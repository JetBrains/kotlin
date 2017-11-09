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

abstract class Statement : Element() {
    object Empty : Statement() {
        override fun generateCode(builder: CodeBuilder) { }
        override val isEmpty: Boolean get() = true
    }
}

class DeclarationStatement(val elements: List<Element>) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(elements, "\n")
    }
}

class ExpressionListStatement(val expressions: List<Expression>) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(expressions, "\n")
    }
}

class LabeledStatement(val name: Identifier, val statement: Element) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append name append "@" append " " append statement
    }
}

class ReturnStatement(val expression: Expression, val label: Identifier? = null) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "return"
        if (label != null) {
            builder append "@" append label
        }
        builder append " " append expression
    }
}

class IfStatement(
        val condition: Expression,
        val thenStatement: Element,
        val elseStatement: Element,
        singleLine: Boolean
) : Expression() {

    private val br = if (singleLine) " " else "\n"
    private val brAfterElse = if (singleLine || elseStatement is IfStatement) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "if (" append condition append ")" append br append thenStatement.wrapToBlockIfRequired()
        if (!elseStatement.isEmpty) {
            builder append br append "else" append brAfterElse append elseStatement.wrapToBlockIfRequired()
        }
        else if (thenStatement.isEmpty) {
            builder append ";"
        }
    }
}

// Loops --------------------------------------------------------------------------------------------------

class WhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "while (" append condition append ")" append br append body.wrapToBlockIfRequired()
        if (body.isEmpty) {
            builder append ";"
        }
    }
}

class DoWhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "do" append br append body.wrapToBlockIfRequired() append br append "while (" append condition append ")"
    }
}

class ForeachStatement(
        val variableName: Identifier,
        val explicitVariableType: Type?,
        val collection: Expression,
        val body: Element,
        singleLine: Boolean
) : Statement() {

    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "for (" append variableName
        if (explicitVariableType != null) {
            builder append ":" append explicitVariableType
        }
        builder append " in " append collection append ")" append br append body.wrapToBlockIfRequired()
        if (body.isEmpty) {
            builder append ";"
        }
    }
}

class BreakStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("break").appendWithPrefix(label, "@")
    }
}

class ContinueStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("continue").appendWithPrefix(label, "@")
    }
}

// Exceptions ----------------------------------------------------------------------------------------------

class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("try\n").append(block).append("\n").append(catches, "\n").append("\n")
        if (!finallyBlock.isEmpty) {
            builder append "finally\n" append finallyBlock
        }
    }
}

class ThrowStatement(val expression: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "throw " append expression
    }
}

class CatchStatement(val variable: FunctionParameter, val block: Block) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "catch (" append variable append ") " append block
    }
}

// when --------------------------------------------------------------------------------------------------

class WhenStatement(val subject: Expression, val caseContainers: List<WhenEntry>) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("when (").append(subject).append(") {\n").append(caseContainers, "\n").append("\n}")
    }
}

class WhenEntry(val selectors: List<WhenEntrySelector>, val body: Statement) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(selectors, ", ").append(" -> ").append(body)
    }
}

abstract class WhenEntrySelector : Statement()

class ValueWhenEntrySelector(val expression: Expression) : WhenEntrySelector() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(expression)
    }
}

class ElseWhenEntrySelector : WhenEntrySelector() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("else")
    }
}

// Other ------------------------------------------------------------------------------------------------------

class SynchronizedStatement(val expression: Expression, val block: Block) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "synchronized (" append expression append ") " append block
    }
}
