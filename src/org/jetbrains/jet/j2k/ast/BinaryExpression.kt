package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.util.AstUtil
import java.util.Arrays
import java.util.List

public open class BinaryExpression(val left: Expression, val right: Expression, val op: String): Expression() {
    public override fun toKotlin(): String = left.toKotlin() + " " + op + " " + right.toKotlin()
}
