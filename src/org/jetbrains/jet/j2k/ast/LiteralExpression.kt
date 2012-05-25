package org.jetbrains.jet.j2k.ast


public open class LiteralExpression(val identifier: Identifier): Expression() {
    public override fun toKotlin() = identifier.toKotlin()
}
