package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.ast.types.VarArg

public open class Parameter(val identifier : Identifier, val `type` : Type, val readOnly: Boolean = true) : Expression() {
    public override fun toKotlin() : String {
        val vararg : String = (if (`type` is VarArg)
            "vararg "
        else
            "")
        val `var` : String? = (if (readOnly)
            ""
        else
            "var ")
        return vararg + `var` + identifier.toKotlin() + " : " + `type`.toKotlin()
    }
}
