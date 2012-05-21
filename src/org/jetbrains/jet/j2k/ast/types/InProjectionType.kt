package org.jetbrains.jet.j2k.ast.types

public open class InProjectionType(val bound : Type) : Type(false) {
    public override fun toKotlin() : String = "in " + bound.toKotlin()
}
