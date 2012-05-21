package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.util.AstUtil
import java.util.List
import java.util.Set

public open class Enum(converter : Converter,
                       name : Identifier,
                       modifiers : Set<String?>,
                       typeParameters : List<Element>,
                       extendsTypes : List<Type>,
                       baseClassParams : List<Expression>,
                       implementsTypes : List<Type>,
                       members : List<Member>) : Class(converter, name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members) {

    override fun primaryConstructorSignatureToKotlin() : String {
        val s : String = super.primaryConstructorSignatureToKotlin()
        return if (s.equals("()")) "" else s
    }

    override fun needOpenModifier() : Boolean = false

    public override fun toKotlin() : String {
        return modifiersToKotlin() +
            "enum class " + name.toKotlin() +
            primaryConstructorSignatureToKotlin() +
            typeParametersToKotlin() +
            implementTypesToKotlin() +
            " {\n" + AstUtil.joinNodes(membersExceptConstructors(), "\n") + "\n" +
            primaryConstructorBodyToKotlin() +
            "\npublic fun name()  : String { return \"\" }\npublic fun order() : Int { return 0 }\n}"
    }
}
