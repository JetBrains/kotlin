package org.jetbrains.jet.j2k.ast

public open class ArrayAccessExpression(val expression : Expression, val index : Expression) : Expression() {
    public override fun toKotlin() : String {
        return expression.toKotlin() + "[" + index.toKotlin() + "]"
    }
}
