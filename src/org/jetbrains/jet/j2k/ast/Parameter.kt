package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class Parameter(val identifier : Identifier, val `type` : Type, val readOnly: Boolean = false) : Expression() {
    public override fun toKotlin() : String {
        val vararg : String = (if (`type`.getKind() == INode.Kind.VARARG)
            "vararg" + " "
        else
            "")
        val `var` : String? = (if (readOnly)
            ""
        else
            "var" + " ")
        return vararg + `var` + identifier.toKotlin() + " : " + `type`.toKotlin()
    }
}
