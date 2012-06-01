package org.jetbrains.jet.j2k.ast

import java.util.List
import java.util.Set
import org.jetbrains.jet.j2k.ast.types.Type

public open class Constructor(identifier : Identifier,
                              docComments: List<Node>,
                              modifiers : Set<Modifier>,
                              `type` : Type,
                              typeParameters : List<Element>,
                              params : Element,
                              block : Block,
                              val isPrimary : Boolean) : Function(identifier, docComments, modifiers, `type`, typeParameters, params, block) {

    public open fun primarySignatureToKotlin() : String {
        return "(" + params.toKotlin() + ")"
    }

    public open fun primaryBodyToKotlin() : String {
        return block!!.toKotlin()
    }
}
