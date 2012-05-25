package org.jetbrains.jet.j2k.ast


public open class PrefixOperator(val op: String, val expression: Expression): Expression() {
    public override fun toKotlin() = op + expression.toKotlin()
    public override fun isNullable() = expression.isNullable()
}
