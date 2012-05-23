package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class TypeCastExpression(val `type` : Type, val expression : Expression) : Expression() {
    public override fun toKotlin() = "(" + expression.toKotlin() + " as " + `type`.toKotlin() + ")"
}
