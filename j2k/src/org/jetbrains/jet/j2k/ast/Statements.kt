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


abstract class Statement() : Element() {
    object Empty : Statement() {
        override fun toKotlin() = ""

        override val isEmpty: Boolean
            get() = true
    }
}

class DeclarationStatement(val elements: List<Element>) : Statement() {
    override fun toKotlin(): String
            = elements.filterIsInstance(javaClass<LocalVariable>()).map { it.toKotlin() }.makeString("\n")
}

class ExpressionListStatement(val expressions: List<Expression>) : Expression() {
    override fun toKotlin() = expressions.toKotlin("\n")
}

class LabelStatement(val name: Identifier, val statement: Element) : Statement() {
    override fun toKotlin(): String = "@" + name.toKotlin() + " " + statement.toKotlin()
}

class ReturnStatement(val expression: Expression) : Statement() {
    override fun toKotlin() = "return " + expression.toKotlin()
}

class IfStatement(
        val condition: Expression,
        val thenStatement: Element,
        val elseStatement: Element,
        singleLine: Boolean
) : Expression() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlin(): String {
        val result = "if (" + condition.toKotlin() + ")$br" + thenStatement.toKotlin()
        if (!elseStatement.isEmpty) {
            return "$result${br}else$br${elseStatement.toKotlin()}"
        }
        return result
    }
}

// Loops --------------------------------------------------------------------------------------------------

class WhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlin() = "while (" + condition.toKotlin() + ")$br" + body.toKotlin()
}

class DoWhileStatement(val condition: Expression, val body: Element, singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlin() = "do$br" + body.toKotlin() + "${br}while (" + condition.toKotlin() + ")"
}

class ForeachStatement(
        val variable: Parameter,
        val expression: Expression,
        val body: Element,
        singleLine: Boolean
) : Statement() {

    private val br = if (singleLine) " " else "\n"

    override fun toKotlin() = "for (" + variable.identifier.toKotlin() + " in " + expression.toKotlin() + ")$br" + body.toKotlin()
}

class ForeachWithRangeStatement(val identifier: Identifier,
                                val start: Expression,
                                val end: Expression,
                                val body: Element,
                                singleLine: Boolean) : Statement() {
    private val br = if (singleLine) " " else "\n"

    override fun toKotlin() = "for (" + identifier.toKotlin() + " in " + start.toKotlin() + ".." + end.toKotlin() + ")$br" + body.toKotlin()
}

class BreakStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlin() = "break" + label.withPrefix("@")
}

class ContinueStatement(val label: Identifier = Identifier.Empty) : Statement() {
    override fun toKotlin() = "continue" + label.withPrefix("@")
}

// Exceptions ----------------------------------------------------------------------------------------------

class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block) : Statement() {
    override fun toKotlin(): String {
        val builder = StringBuilder()
                .append("try\n")
                .append(block.toKotlin())
                .append("\n")
                .append(catches.toKotlin("\n"))
                .append("\n")
        if (!finallyBlock.isEmpty) {
            builder.append("finally\n").append(finallyBlock.toKotlin())
        }
        return builder.toString()
    }
}

class ThrowStatement(val expression: Expression) : Expression() {
    override fun toKotlin() = "throw " + expression.toKotlin()
}

class CatchStatement(val variable: Parameter, val block: Block) : Statement() {
    override fun toKotlin(): String = "catch (" + variable.toKotlin() + ") " + block.toKotlin()
}

// Switch --------------------------------------------------------------------------------------------------

class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>) : Statement() {
    override fun toKotlin() = "when (" + expression.toKotlin() + ") {\n" + caseContainers.toKotlin("\n") + "\n}"
}

class CaseContainer(val caseStatement: List<Element>, statements: List<Statement>) : Statement() {
    private val block = Block(statements.filterNot { it is BreakStatement || it is ContinueStatement }, true)

    override fun toKotlin() = caseStatement.toKotlin(", ") + " -> " + block.toKotlin()
}

class SwitchLabelStatement(val expression: Expression) : Statement() {
    override fun toKotlin() = expression.toKotlin()
}

class DefaultSwitchLabelStatement() : Statement() {
    override fun toKotlin() = "else"
}

// Other ------------------------------------------------------------------------------------------------------

class SynchronizedStatement(val expression: Expression, val block: Block) : Statement() {
    override fun toKotlin() = "synchronized (" + expression.toKotlin() + ") " + block.toKotlin()
}

class StatementList(elements: List<Element>)
  : WhiteSpaceSeparatedElementList(elements, WhiteSpace.NewLine) {
    val statements: List<Statement>
        get() = elements.filterIsInstance(javaClass<Statement>())
}