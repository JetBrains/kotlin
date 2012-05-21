package org.jetbrains.jet.j2k.ast.types

public open class StarProjectionType() : Type(false) {
    public override fun toKotlin() : String = "*"
}
