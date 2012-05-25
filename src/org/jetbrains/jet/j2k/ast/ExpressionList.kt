package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.util.AstUtil
import java.util.List

public open class ExpressionList(val expressions: List<Expression>): Expression() {
    public override fun toKotlin(): String = expressions.map { it.toKotlin() }.makeString(", ")
}
