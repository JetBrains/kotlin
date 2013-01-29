package org.jetbrains.jet.j2k.ast

public open class CallChainExpression(val expression : Expression, val identifier : Expression) : Expression() {
    public override fun isNullable() : Boolean {
        if (!expression.isEmpty() && expression.isNullable()) return true
        return identifier.isNullable()
    }

    public override fun toKotlin() : String {
        if (!expression.isEmpty()) {
            return expression.toKotlin() + (if (expression.isNullable()) "?." else ".") + identifier.toKotlin()
        }

        return identifier.toKotlin()
    }
}
