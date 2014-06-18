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

import org.jetbrains.jet.j2k.CommentsAndSpaces


abstract class Statement() : Element() {
    object Empty : Statement() {
        override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = ""

        override val isEmpty: Boolean
            get() = true
    }
}

class DeclarationStatement(val elements: List<Element>) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String
            = elements.filterIsInstance(javaClass<LocalVariable>()).map { it.toKotlin(commentsAndSpaces) }.makeString("\n")
}

class ExpressionListStatement(val expressions: List<Expression>) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = expressions.toKotlin(commentsAndSpaces, "\n")
}

class LabelStatement(val name: Identifier, val statement: Element) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "@" + name.toKotlin(commentsAndSpaces) + " " + statement.toKotlin(commentsAndSpaces)
}

class ReturnStatement(val expression: Expression) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "return " + expression.toKotlin(commentsAndSpaces)
}

class IfStatement(
        val condition: Expression,
        val thenStatement: Element,
        val elseStatement: Element,
        singleLine: Boolean
) : Expression() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        val result = "if (" + condition.toKotlin(commentsAndSpaces) + ")$br" + thenStatement.toKotlin(commentsAndSpaces)
        if (!elseStatement.isEmpty) {
            return "$result${br}else$br${elseStatement.toKotlin(commentsAndSpaces)}"
        }
        return result
    }
}

// Loops --------------------------------------------------------------------------------------------------

class WhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "while (" + condition.toKotlin(commentsAndSpaces) + ")$br" + body.toKotlin(commentsAndSpaces)
}

class DoWhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "do$br" + body.toKotlin(commentsAndSpaces) + "${br}while (" + condition.toKotlin(commentsAndSpaces) + ")"
}

class ForeachStatement(
        val variable: Parameter,
        val expression: Expression,
        val body: Element,
        singleLine: Boolean
) : Statement() {

    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "for (" + variable.identifier.toKotlin(commentsAndSpaces) + " in " + expression.toKotlin(commentsAndSpaces) + ")$br" + body.toKotlin(commentsAndSpaces)
}

class ForeachWithRangeStatement(val identifier: Identifier,
                                val start: Expression,
                                val end: Expression,
                                val body: Element,
                                singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "for (" + identifier.toKotlin(commentsAndSpaces) + " in " + start.toKotlin(commentsAndSpaces) + ".." + end.toKotlin(commentsAndSpaces) + ")$br" + body.toKotlin(commentsAndSpaces)
}

class BreakStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "break" + label.withPrefix("@", commentsAndSpaces)
}

class ContinueStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "continue" + label.withPrefix("@", commentsAndSpaces)
}

// Exceptions ----------------------------------------------------------------------------------------------

class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        val builder = StringBuilder()
                .append("try\n")
                .append(block.toKotlin(commentsAndSpaces))
                .append("\n")
                .append(catches.toKotlin(commentsAndSpaces, "\n"))
                .append("\n")
        if (!finallyBlock.isEmpty) {
            builder.append("finally\n").append(finallyBlock.toKotlin(commentsAndSpaces))
        }
        return builder.toString()
    }
}

class ThrowStatement(val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "throw " + expression.toKotlin(commentsAndSpaces)
}

class CatchStatement(val variable: Parameter, val block: Block) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "catch (" + variable.toKotlin(commentsAndSpaces) + ") " + block.toKotlin(commentsAndSpaces)
}

// Switch --------------------------------------------------------------------------------------------------

class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "when (" + expression.toKotlin(commentsAndSpaces) + ") {\n" + caseContainers.toKotlin(commentsAndSpaces, "\n") + "\n}"
}

class CaseContainer(val caseStatement: List<Element>, statements: List<Statement>) : Statement() {
    private val block = Block(statements.filterNot { it is BreakStatement || it is ContinueStatement }, LBrace(), RBrace(), true)

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = caseStatement.toKotlin(commentsAndSpaces, ", ") + " -> " + block.toKotlin(commentsAndSpaces)
}

class SwitchLabelStatement(val expression: Expression) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = expression.toKotlin(commentsAndSpaces)
}

class DefaultSwitchLabelStatement() : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "else"
}

// Other ------------------------------------------------------------------------------------------------------

class SynchronizedStatement(val expression: Expression, val block: Block) : Statement() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "synchronized (" + expression.toKotlin(commentsAndSpaces) + ") " + block.toKotlin(commentsAndSpaces)
}
