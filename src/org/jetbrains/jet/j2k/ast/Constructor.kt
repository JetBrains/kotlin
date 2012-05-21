package org.jetbrains.jet.j2k.ast

import java.util.List
import java.util.Set
import org.jetbrains.jet.j2k.ast.INode.Kind

public open class Constructor(identifier : Identifier,
                              modifiers : Set<String?>,
                              `type` : Type,
                              typeParameters : List<Element>,
                              params : Element,
                              block : Block,
                              val isPrimary : Boolean) : Function(identifier, modifiers, `type`, typeParameters, params, block) {

    public open fun primarySignatureToKotlin() : String {
        return "(" + params.toKotlin() + ")"
    }

    public open fun primaryBodyToKotlin() : String {
        return block!!.toKotlin()
    }

    public override fun getKind() : INode.Kind {
        return INode.Kind.CONSTRUCTOR
    }
}
