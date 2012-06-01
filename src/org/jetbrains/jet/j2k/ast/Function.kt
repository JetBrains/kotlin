package org.jetbrains.jet.j2k.ast

import java.util.LinkedList
import java.util.List
import java.util.Set
import org.jetbrains.jet.j2k.ast.types.Type

public open class Function(val name : Identifier,
                           val docComments: List<Node>,
                           modifiers : Set<String>,
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
        val modifierList: List<String> = arrayList()
        val accessModifier : String = accessModifier()
        if (!accessModifier.isEmpty()) {
            modifierList.add(accessModifier)
        }

        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        if (myModifiers.contains(Modifier.OVERRIDE)) {
            modifierList.add(Modifier.OVERRIDE)
        }

        if (!myModifiers.contains(Modifier.ABSTRACT) &&
            !myModifiers.contains(Modifier.OVERRIDE) &&
            !myModifiers.contains(Modifier.FINAL) &&
            !myModifiers.contains(Modifier.PRIVATE))
        {
            modifierList.add(Modifier.OPEN)
        }

        if (myModifiers.contains(Modifier.NOT_OPEN)) {
            modifierList.remove(Modifier.OPEN)
        }

        if (modifierList.size() > 0) {
            return modifierList.makeString(" ") + " "
        }

        return ""
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
