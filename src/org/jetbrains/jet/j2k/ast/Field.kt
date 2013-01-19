package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.LinkedList
import org.jetbrains.jet.j2k.Converter

public open class Field(val identifier : Identifier,
                        val docComments: List<Node>,
                        modifiers : Set<Modifier>,
                        val `type` : Type,
                        val initializer : Element,
                        val writingAccesses : Int) : Member(modifiers) {

    open fun modifiersToKotlin() : String {
        val modifierList : List<Modifier> = arrayList()
        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        val modifier = accessModifier()
        if (modifier != null) {
          modifierList.add(modifier)
        }

        return modifierList.toKotlin() + (if (isVal()) "val " else "var ")
    }

    public open fun isVal() : Boolean = modifiers.contains(Modifier.FINAL)
    public override fun isStatic() : Boolean = modifiers.contains(Modifier.STATIC)

    public override fun toKotlin() : String {
        val declaration : String = docComments.toKotlin("\n", "", "\n") +
                                   modifiersToKotlin() + identifier.toKotlin() + " : " + `type`.toKotlin()
        if (initializer.isEmpty()) {
            return declaration + ((if (isVal() && !isStatic() && writingAccesses == 1)
                ""
            else
                " = " + Converter.getDefaultInitializer(this)))
        }

        return declaration + " = " + initializer.toKotlin()
    }
}
