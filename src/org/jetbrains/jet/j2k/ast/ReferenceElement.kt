package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.util.AstUtil
import java.util.List

public open class ReferenceElement(val reference : Identifier, val types : List<Type>) : Element() {
    public override fun toKotlin() : String {
        val typesIfNeeded : String = (if (types.size() > 0)
            "<" + AstUtil.joinNodes(types, ", ") + ">"
        else
            "")
        return reference.toKotlin() + typesIfNeeded
    }
}
