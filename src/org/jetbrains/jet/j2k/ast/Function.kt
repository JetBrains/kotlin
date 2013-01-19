package org.jetbrains.jet.j2k.ast

import java.util.LinkedList
import org.jetbrains.jet.j2k.ast.types.Type

public open class Function(val name : Identifier,
                           val docComments: List<Node>,
                           modifiers : Set<Modifier>,
                           val `type` : Type,
                           val typeParameters : List<Element>,
                           val params : Element,
                           var block : Block?) : Member(modifiers) {
    private fun typeParametersToKotlin() : String {
        return (if (typeParameters.size() > 0)
            "<" + typeParameters.map { it.toKotlin() }.makeString(", ") + ">"
        else
            "")
    }

    private fun hasWhere() : Boolean = typeParameters.any { it is TypeParameter && it.hasWhere() }

    private fun typeParameterWhereToKotlin() : String {
        if (hasWhere())
        {
            val wheres = typeParameters.filter { it is TypeParameter }.map { ((it as TypeParameter).getWhereToKotlin() )}
            return " where " + wheres.makeString(", ") + " "
        }

        return ""
    }

    open fun modifiersToKotlin() : String {
        val modifierList: List<Modifier> = arrayList()
        val accessModifier = accessModifier()
        if (accessModifier != null) {
            modifierList.add(accessModifier)
        }

        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OVERRIDE)) {
            modifierList.add(Modifier.OVERRIDE)
        }

        if (!modifiers.contains(Modifier.ABSTRACT) &&
            !modifiers.contains(Modifier.OVERRIDE) &&
            !modifiers.contains(Modifier.FINAL) &&
            !modifiers.contains(Modifier.PRIVATE)) {
            modifierList.add(Modifier.OPEN)
        }

        if (modifiers.contains(Modifier.NOT_OPEN)) {
            modifierList.remove(Modifier.OPEN)
        }

        return modifierList.toKotlin()
    }

    public override fun toKotlin() : String {
        return docComments.toKotlin("\n", "", "\n") +
               modifiersToKotlin() +
               "fun " + name.toKotlin() +
               typeParametersToKotlin() +
               "(" + params.toKotlin() + ") : " +
               `type`.toKotlin() + " "+ typeParameterWhereToKotlin() +
               block?.toKotlin()
    }
}
