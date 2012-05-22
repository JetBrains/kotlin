package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.Set

public open class EnumConstant(identifier : Identifier,
                               modifiers : Set<String?>,
                               `type` : Type,
                               params : Element) : Field(identifier, modifiers, `type`.convertedToNotNull(), params, 0) {

    public override fun toKotlin() : String {
        if (initializer.toKotlin().isEmpty()) {
            return identifier.toKotlin()
        }

        return identifier.toKotlin() + " : " + `type`.toKotlin() + "(" + initializer.toKotlin() + ")"
    }


}
