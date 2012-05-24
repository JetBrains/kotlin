package org.jetbrains.jet.j2k.ast

public class BangBangExpression(val expr: Expression): Expression() {
    public override fun toKotlin(): String = expr.toKotlin() + "!!"
}
