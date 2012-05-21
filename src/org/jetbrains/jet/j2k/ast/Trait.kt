package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.List
import java.util.Set

public open class Trait(converter : Converter,
                        name : Identifier,
                        modifiers : Set<String?>,
                        typeParameters : List<Element>,
                        extendsTypes : List<Type>,
                        baseClassParams : List<Expression>,
                        implementsTypes : List<Type>,
                        members : List<Member>) : Class(converter, name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members) {

    override val TYPE: String
        get() = "trait"

    override fun primaryConstructorSignatureToKotlin() = ""
    override fun needOpenModifier() = false
}
