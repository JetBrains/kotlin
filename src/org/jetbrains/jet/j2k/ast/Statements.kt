package org.jetbrains.jet.j2k.ast

import java.util.List

public open class DeclarationStatement(val elements: List<Element>): Statement() {
    public override fun toKotlin(): String {
        return elements.filter { it is LocalVariable }.map { convertDeclaration(it as LocalVariable) }.makeString("\n")
    }

    private fun convertDeclaration(v: LocalVariable): String {
        val varKeyword: String? = (if (v.hasModifier(Modifier.FINAL))
            "val"
        else
            "var")
        return varKeyword + " " + v.toKotlin()
    }
}

public open class ExpressionListStatement(val expressions: List<Expression>): Expression() {
    public override fun toKotlin() = expressions.toKotlin("\n")
}

public open class LabelStatement(val name: Identifier, val statement: Statement): Statement() {
    public override fun toKotlin(): String = "@" + name.toKotlin() + " " + statement.toKotlin()
}

public open class ReturnStatement(val expression: Expression): Statement() {
    public override fun toKotlin() = "return " + expression.toKotlin()
}

// Loops --------------------------------------------------------------------------------------------------

public open class WhileStatement(val condition: Expression, val statement: Statement): Statement() {
    public override fun toKotlin() = "while (" + condition.toKotlin() + ")\n" + statement.toKotlin()
}

public open class DoWhileStatement(condition: Expression, statement: Statement): WhileStatement(condition, statement) {
    public override fun toKotlin() = "do\n" + statement.toKotlin() + "\nwhile (" + condition.toKotlin() + ")"
}

public open class BreakStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER) : Statement() {
    public override fun toKotlin() = "break" + label.withPrefix("@")
}

public open class ContinueStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER): Statement() {
    public override fun toKotlin() = "continue" + label.withPrefix("@")
}

// Exceptions ----------------------------------------------------------------------------------------------

public open class TryStatement(val block: Block, val catches: List<CatchStatement>, val finallyBlock: Block): Statement() {
    public override fun toKotlin(): String {
        return "try\n" + block.toKotlin() + "\n" + catches.toKotlin("\n") + "\n" + (if (finallyBlock.isEmpty())
            ""
        else
            "finally\n" + finallyBlock.toKotlin())
    }
}

public open class ThrowStatement(val expression: Expression): Expression() {
    public override fun toKotlin() = "throw " + expression.toKotlin()
}

public open class CatchStatement(val variable: Parameter, val block: Block): Statement() {
    public override fun toKotlin(): String =  "catch (" + variable.toKotlin() + ") " + block.toKotlin()
}

// Switch --------------------------------------------------------------------------------------------------

public open class SwitchContainer(val expression: Expression, val caseContainers: List<CaseContainer>): Statement() {
    public override fun toKotlin() = "when (" + expression.toKotlin() + ") {\n" + caseContainers.toKotlin("\n") + "\n}"
}

public open class CaseContainer(val caseStatement: List<Statement>, statements: List<Statement>): Statement() {
    private val myBlock: Block

    {
        val newStatements: List<Statement> = statements.filterNot { it is BreakStatement || it is ContinueStatement }
        myBlock = Block(newStatements, false)
    }

    public override fun toKotlin() = caseStatement.toKotlin(", ") + " -> " + myBlock.toKotlin()
}

public open class SwitchLabelStatement(val expression: Expression): Statement() {
    public override fun toKotlin() = expression.toKotlin()
}

public open class DefaultSwitchLabelStatement(): Statement() {
    public override fun toKotlin() = "else"
}
