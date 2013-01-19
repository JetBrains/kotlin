package org.jetbrains.jet.j2k.ast


public open class PolyadicExpression(val expressions: List<Expression>, val token: String): Expression() {
    public override fun toKotlin(): String {
        val expressionsWithConversions = expressions.map { it.toKotlin() }
        return expressionsWithConversions.makeString(" " + token + " ")
    }
}
