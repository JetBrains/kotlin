package org.jetbrains.jet.j2k.ast


public open class DummyStringExpression(val string: String): Expression() {
    public override fun toKotlin(): String = string
}
