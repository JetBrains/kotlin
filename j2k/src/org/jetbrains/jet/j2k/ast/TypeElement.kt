package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class TypeElement(val `type` : Type) : Element() {
    override fun toKotlin() = `type`.toKotlin()

    public fun toKotlinNotNull(): String = `type`.convertedToNotNull().toKotlin()
}
