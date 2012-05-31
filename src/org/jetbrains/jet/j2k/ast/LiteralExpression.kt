package org.jetbrains.jet.j2k.ast


public open class LiteralExpression(val literalText: String): Expression() {
    public override fun toKotlin() = literalText
}
