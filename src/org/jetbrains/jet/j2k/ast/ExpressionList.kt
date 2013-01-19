package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class ExpressionList(val expressions: List<Expression>): Expression() {
    public override fun toKotlin(): String = expressions.map { it.toKotlin() }.makeString(", ")
}
