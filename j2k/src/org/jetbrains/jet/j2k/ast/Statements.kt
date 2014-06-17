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

import org.jetbrains.jet.j2k.CommentConverter


abstract class Statement() : Element() {
    object Empty : Statement() {
        override fun toKotlinImpl(commentConverter: CommentConverter) = ""

        override val isEmpty: Boolean
            get() = true
    }
}

class DeclarationStatement(val elements: List<Element>) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String
            = elements.filterIsInstance(javaClass<LocalVariable>()).map { it.toKotlin(commentConverter) }.makeString("\n")
}

class ExpressionListStatement(val expressions: List<Expression>) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = expressions.toKotlin(commentConverter, "\n")
}

class LabelStatement(val name: Identifier, val statement: Element) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String = "@" + name.toKotlin(commentConverter) + " " + statement.toKotlin(commentConverter)
}

class ReturnStatement(val expression: Expression) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "return " + expression.toKotlin(commentConverter)
}

class IfStatement(
        val condition: Expression,
        val thenStatement: Element,
        val elseStatement: Element,
        singleLine: Boolean
) : Expression() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        val result = "if (" + condition.toKotlin(commentConverter) + ")$br" + thenStatement.toKotlin(commentConverter)
        if (!elseStatement.isEmpty) {
            return "$result${br}else$br${elseStatement.toKotlin(commentConverter)}"
        }
        return result
    }
}

// Loops --------------------------------------------------------------------------------------------------

class WhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentConverter: CommentConverter) = "while (" + condition.toKotlin(commentConverter) + ")$br" + body.toKotlin(commentConverter)
}

class DoWhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentConverter: CommentConverter) = "do$br" + body.toKotlin(commentConverter) + "${br}while (" + condition.toKotlin(commentConverter) + ")"
}

class ForeachStatement(
        val variable: Parameter,
        val expression: Expression,
        val body: Element,
        singleLine: Boolean
) : Statement() {

    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentConverter: CommentConverter) = "for (" + variable.identifier.toKotlin(commentConverter) + " in " + expression.toKotlin(commentConverter) + ")$br" + body.toKotlin(commentConverter)
}

class ForeachWithRangeStatement(val identifier: Identifier,
                                val start: Expression,
                                val end: Expression,
                                val body: Element,
                                singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlinImpl(commentConverter: CommentConverter) = "for (" + identifier.toKotlin(commentConverter) + " in " + start.toKotlin(commentConverter) + ".." + end.toKotlin(commentConverter) + ")$br" + body.toKotlin(commentConverter)
}

class BreakStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "break" + label.withPrefix("@", commentConverter)
}

class ContinueStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "continue" + label.withPrefix("@", commentConverter)
}

// Exceptions ----------------------------------------------------------------------------------------------

class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        val builder = StringBuilder()
                .append("try\n")
                .append(block.toKotlin(commentConverter))
                .append("\n")
                .append(catches.toKotlin(commentConverter, "\n"))
                .append("\n")
        if (!finallyBlock.isEmpty) {
            builder.append("finally\n").append(finallyBlock.toKotlin(commentConverter))
        }
        return builder.toString()
    }
}

class ThrowStatement(val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "throw " + expression.toKotlin(commentConverter)
}

class CatchStatement(val variable: Parameter, val block: Block) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String = "catch (" + variable.toKotlin(commentConverter) + ") " + block.toKotlin(commentConverter)
}

// Switch --------------------------------------------------------------------------------------------------

class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "when (" + expression.toKotlin(commentConverter) + ") {\n" + caseContainers.toKotlin(commentConverter, "\n") + "\n}"
}

class CaseContainer(val caseStatement: List<Element>, statements: List<Statement>) : Statement() {
    private val block = Block(statements.filterNot { it is BreakStatement || it is ContinueStatement }, true)

    override fun toKotlinImpl(commentConverter: CommentConverter) = caseStatement.toKotlin(commentConverter, ", ") + " -> " + block.toKotlin(commentConverter)
}

class SwitchLabelStatement(val expression: Expression) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = expression.toKotlin(commentConverter)
}

class DefaultSwitchLabelStatement() : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "else"
}

// Other ------------------------------------------------------------------------------------------------------

class SynchronizedStatement(val expression: Expression, val block: Block) : Statement() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "synchronized (" + expression.toKotlin(commentConverter) + ") " + block.toKotlin(commentConverter)
}

class StatementList(elements: List<Element>)
  : WhiteSpaceSeparatedElementList(elements, WhiteSpace.NewLine) {
    val statements: List<Statement>
        get() = elements.filterIsInstance(javaClass<Statement>())
}