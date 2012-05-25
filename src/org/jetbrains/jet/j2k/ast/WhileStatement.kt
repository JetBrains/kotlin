package org.jetbrains.jet.j2k.ast


public open class WhileStatement(val condition: Expression, val statement: Statement): Statement() {
    public override fun toKotlin(): String {
        return "while (" + condition.toKotlin() + ")\n" + statement.toKotlin()
    }
}
