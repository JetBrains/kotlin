package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.Identifier

public open class PrimitiveType(val `type` : Identifier) : Type(false) {
    public override fun toKotlin() : String = `type`.toKotlin()
}
