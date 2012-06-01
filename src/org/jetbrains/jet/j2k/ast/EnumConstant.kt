package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.Set
import java.util.List

public open class EnumConstant(identifier : Identifier,
                               docComments: List<Node>,
                               modifiers : Set<Modifier>,
                               `type` : Type,
                               params : Element) : Field(identifier, docComments, modifiers, `type`.convertedToNotNull(), params, 0) {

    public override fun toKotlin() : String {
        if (initializer.toKotlin().isEmpty()) {
            return identifier.toKotlin()
        }

        return identifier.toKotlin() + " : " + `type`.toKotlin() + "(" + initializer.toKotlin() + ")"
    }


}
