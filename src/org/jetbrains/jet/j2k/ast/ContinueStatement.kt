package org.jetbrains.jet.j2k.ast


public open class ContinueStatement(val label: Identifier = Identifier.EMPTY_IDENTIFIER): Statement() {
    public override fun toKotlin(): String {
        if (label.isEmpty()) {
            return "continue"
        }

        return "continue@" + label.toKotlin()
    }
}
