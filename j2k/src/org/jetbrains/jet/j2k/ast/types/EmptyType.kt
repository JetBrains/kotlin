package org.jetbrains.jet.j2k.ast.types

public open class EmptyType() : Type(false) {
    public override fun toKotlin() : String = "UNRESOLVED_TYPE"
}
