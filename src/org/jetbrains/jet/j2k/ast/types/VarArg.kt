package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.types.Type

public open class VarArg(val `type` : Type) : Type(false) {
    public override fun toKotlin() : String = `type`.toKotlin()
}
