package org.jetbrains.jet.j2k.ast


public open class DefaultSwitchLabelStatement(): Statement() {
    public override fun toKotlin() = "else"
}
