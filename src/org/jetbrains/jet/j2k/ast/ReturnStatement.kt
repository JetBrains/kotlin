package org.jetbrains.jet.j2k.ast

public open class ReturnStatement(val expression: Expression): Statement() {
    public override fun toKotlin() = "return " + expression.toKotlin()
}
