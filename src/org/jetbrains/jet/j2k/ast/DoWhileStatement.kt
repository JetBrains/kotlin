package org.jetbrains.jet.j2k.ast


public open class DoWhileStatement(condition: Expression, statement: Statement): WhileStatement(condition, statement) {
    public override fun toKotlin(): String {
        return "do\n" + statement.toKotlin() + "\nwhile (" + condition.toKotlin() + ")"
    }
}
