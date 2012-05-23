package org.jetbrains.jet.j2k.ast

import a.h.id


public open class ThisExpression(val identifier: Identifier) : Expression() {
    public override fun toKotlin() : String {
        return if (identifier.isEmpty()) "this" else "this@" + identifier.toKotlin()
    }
}
