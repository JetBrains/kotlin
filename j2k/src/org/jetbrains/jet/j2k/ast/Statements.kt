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


public abstract class Statement() : Element() {
    class object {
        public val EMPTY_STATEMENT: Statement = object : Statement() {
            public override fun toKotlin() = ""
        }
    }
}

public open class DeclarationStatement(val elements: List<Element>) : Statement() {
    public override fun toKotlin(): String {
        return elements.filter { it is LocalVariable }.map { convertDeclaration(it as LocalVariable) }.makeString("\n")
    }

    private fun convertDeclaration(v: LocalVariable): String {
        val varKeyword: String? = (if (v.isImmutable())
            "val"
        else
            "var")
        return varKeyword + " " + v.toKotlin()
    }
}

public open class ExpressionListStatement(val expressions: List<Expression>) : Expression() {
    public override fun toKotlin() = expressions.toKotlin("\n")
}

public open class LabelStatement(val name: Identifier, val statement: Element) : Statement() {
    public override fun toKotlin(): String = "@" + name.toKotlin() + " " + statement.toKotlin()
}

public open class ReturnStatement(val expression: Expression) : Statement() {
    public override fun toKotlin() = "return " + expression.toKotlin()
}

public open class IfStatement(val condition: Expression,
                              val thenStatement: Element,
                              val elseStatement: Element) : Expression() {
    public override fun toKotlin(): String {
        val result: String = "if (" + condition.toKotlin() + ")\n" + thenStatement.toKotlin() + "\n"
        if (elseStatement != Statement.EMPTY_STATEMENT) {
            return result + "else\n" + elseStatement.toKotlin()
        }

        return result
    }
}

// Loops --------------------------------------------------------------------------------------------------

public open class WhileStatement(val condition: Expression, val body: Element) : Statement() {
    public override fun toKotlin() = "while (" + condition.toKotlin() + ")\n" + body.toKotlin()
}

public open class DoWhileStatement(condition: Expression, body: Element) : WhileStatement(condition, body) {
    public override fun toKotlin() = "do\n" + body.toKotlin() + "\nwhile (" + condition.toKotlin() + ")"
}

public open class ForeachStatement(val variable: Parameter,
                                   val expression: Expression,
                                   val body: Element) : Statement() {
    public override fun toKotlin() = "for (" + variable.identifier.name + " in " +
    expression.toKotlin() + ")\n" + body.toKotlin()
}

public open class ForeachWithRangeStatement(val identifier: Identifier,
                                            val start: Expression,
                                            val end: Expression,
                                            val body: Element) : Statement() {
    public override fun toKotlin() = "for (" + identifier.toKotlin() + " in " +
    start.toKotlin() + ".." + end.toKotlin() + ") " + body.toKotlin()
}

public open class BreakStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER) : Statement() {
    public override fun toKotlin() = "break" + label.withPrefix("@")
}

public open class ContinueStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER) : Statement() {
    public override fun toKotlin() = "continue" + label.withPrefix("@")
}

// Exceptions ----------------------------------------------------------------------------------------------

public open class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block) : Statement() {
    public override fun toKotlin(): String {
        return "try\n" + block.toKotlin() + "\n" + catches.toKotlin("\n") + "\n" + (if (finallyBlock.isEmpty())
            ""
        else
            "finally\n" + finallyBlock.toKotlin())
    }
}

public open class ThrowStatement(val expression: Expression) : Expression() {
    public override fun toKotlin() = "throw " + expression.toKotlin()
}

public open class CatchStatement(val variable: Parameter, val block: Block) : Statement() {
    public override fun toKotlin(): String = "catch (" + variable.toKotlin() + ") " + block.toKotlin()
}

// Switch --------------------------------------------------------------------------------------------------

public open class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>) : Statement() {
    public override fun toKotlin() = "when (" + expression.toKotlin() + ") {\n" + caseContainers.toKotlin("\n") + "\n}"
}

public open class CaseContainer(val caseStatement: List<Element>, statements: List<Element>) : Statement() {
    private val myBlock: Block

    {
        val newStatements = statements.filterNot { it is BreakStatement || it is ContinueStatement }
        myBlock = Block(newStatements, true)
    }

    public override fun toKotlin() = caseStatement.toKotlin(", ") + " -> " + myBlock.toKotlin()
}

public open class SwitchLabelStatement(val expression: Expression) : Statement() {
    public override fun toKotlin() = expression.toKotlin()
}

public open class DefaultSwitchLabelStatement() : Statement() {
    public override fun toKotlin() = "else"
}

// Other ------------------------------------------------------------------------------------------------------

public open class SynchronizedStatement(val expression: Expression, val block: Block) : Statement() {
    public override fun toKotlin() = "synchronized (" + expression.toKotlin() + ") " + block.toKotlin()
}
