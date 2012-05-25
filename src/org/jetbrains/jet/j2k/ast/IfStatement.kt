package org.jetbrains.jet.j2k.ast


public open class IfStatement(val condition: Expression, val thenStatement: Statement, val elseStatement: Statement): Expression() {
    public override fun toKotlin(): String {
        val result: String = "if (" + condition.toKotlin() + ")\n" + thenStatement.toKotlin() + "\n"
        if (elseStatement != Statement.EMPTY_STATEMENT) {
            return result + "else\n" + elseStatement.toKotlin()
        }

        return result
    }
}
