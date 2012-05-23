package org.jetbrains.jet.j2k.ast


public open class ParenthesizedExpression(val expression : Expression) : Expression() {
    public override fun toKotlin() = "(" + expression.toKotlin() + ")"
}
