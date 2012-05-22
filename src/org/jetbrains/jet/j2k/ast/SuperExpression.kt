package org.jetbrains.jet.j2k.ast


public open class SuperExpression(val identifier : Identifier) : Expression() {
    public override fun toKotlin() : String {
        if (identifier.isEmpty()) {
            return "super"
        }
        return "super@" + identifier.toKotlin()
    }
}
