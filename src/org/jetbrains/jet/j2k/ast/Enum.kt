package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.Type

public open class Enum(converter : Converter,
                       name : Identifier,
                       docComments: List<Node>,
                       modifiers : Set<Modifier>,
                       typeParameters : List<Element>,
                       extendsTypes : List<Type>,
                       baseClassParams : List<Expression>,
                       implementsTypes : List<Type>,
                       members : List<Node>) : Class(converter, name, docComments, modifiers, typeParameters,
                                                    extendsTypes, baseClassParams, implementsTypes, members) {

    override fun primaryConstructorSignatureToKotlin() : String {
        val s : String = super.primaryConstructorSignatureToKotlin()
        return if (s.equals("()")) "" else s
    }

    override fun needOpenModifier() = false

    public override fun toKotlin() : String {
        val primaryConstructorBody = primaryConstructorBodyToKotlin() ?: ""
        return modifiersToKotlin() +
            "enum class " + name.toKotlin() +
            primaryConstructorSignatureToKotlin() +
            typeParametersToKotlin() +
            implementTypesToKotlin() +
            " {\n" + membersExceptConstructors().toKotlin("\n") + "\n" +
            (if (primaryConstructorBody.isEmpty()) "" else primaryConstructorBody + "\n") +
            "}"
    }
}
