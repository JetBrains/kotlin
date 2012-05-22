package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class TypeElement(val `type` : Type) : Element() {
    public override fun toKotlin() : String {
        return `type`.toKotlin()
    }
}
