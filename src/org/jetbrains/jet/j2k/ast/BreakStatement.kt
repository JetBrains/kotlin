package org.jetbrains.jet.j2k.ast


public open class BreakStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER) : Statement() {
    public override fun toKotlin() : String {
        return if (label.isEmpty()) "break" else "break@" + label.toKotlin()
    }
}
