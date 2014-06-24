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

import org.jetbrains.jet.j2k.*


abstract class Statement() : Element() {
    object Empty : Statement() {
        override fun generateCode(builder: CodeBuilder) { }

        override val isEmpty: Boolean
            get() = true
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

class LabelStatement(val name: Identifier, val statement: Element) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "@" append name append " " append statement
    }
}

class ReturnStatement(val expression: Expression) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "return " append expression
    }
}

class IfStatement(
        val condition: Expression,
        val thenStatement: Element,
        val elseStatement: Element,
        singleLine: Boolean
) : Expression() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "if (" append condition append ")" append br append thenStatement
        if (!elseStatement.isEmpty) {
            builder append br append "else" append br append elseStatement
        }
    }
}

// Loops --------------------------------------------------------------------------------------------------

class WhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "while (" append condition append ")" append br append body
    }
}

class DoWhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "do" append br append body append br append "while (" append condition append ")"
    }
}

class ForeachStatement(
        val variable: Parameter,
        val expression: Expression,
        val body: Element,
        singleLine: Boolean
) : Statement() {

    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "for (" append variable.identifier append " in " append expression append ")" append br append body
    }
}

class ForeachWithRangeStatement(val identifier: Identifier,
                                val start: Expression,
                                val end: Expression,
                                val body: Element,
                                singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun generateCode(builder: CodeBuilder) {
        builder append "for (" append identifier append " in " append start append ".." append end append ")" append br append body
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

class CatchStatement(val variable: Parameter, val block: Block) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "catch (" append variable append ") " append block
    }
}

// Switch --------------------------------------------------------------------------------------------------

class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("when (").append(expression).append(") {\n").append(caseContainers, "\n").append("\n}")
    }
}

class CaseContainer(val caseStatement: List<Element>, statements: List<Statement>) : Statement() {
    private val block = run {
        val filteredStatements = statements.filterNot { it is BreakStatement || it is ContinueStatement }
        Block(filteredStatements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(caseStatement, ", ").append(" -> ").append(block)
    }
}

class SwitchLabelStatement(val expression: Expression) : Statement() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(expression)
    }
}

class DefaultSwitchLabelStatement() : Statement() {
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
