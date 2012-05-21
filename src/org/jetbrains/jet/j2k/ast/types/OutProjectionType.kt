package org.jetbrains.jet.j2k.ast.types

public open class OutProjectionType(val bound : Type) : Type(false) {
    public override fun toKotlin() : String = "out " + bound.toKotlin()
}
