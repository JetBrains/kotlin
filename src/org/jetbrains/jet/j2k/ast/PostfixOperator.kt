package org.jetbrains.jet.j2k.ast


public open class PostfixOperator(val op: String, val expression: Expression): Expression() {
    public override fun toKotlin(): String = expression.toKotlin() + op
}
