package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.LinkedList
import java.util.List
import java.util.Set
import org.jetbrains.jet.j2k.Converter

public open class Field(val identifier : Identifier,
                        modifiers : Set<String>,
                        val `type` : Type,
                        val initializer : Element,
                        val writingAccesses : Int) : Member(modifiers) {

    open fun modifiersToKotlin() : String {
        val modifierList : List<String> = arrayList()
        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        modifierList.add(accessModifier())
        modifierList.add(if (isVal()) "val" else "var")
        if (modifierList.size() > 0)
        {
            return modifierList.makeString(" ") + " "
        }

        return ""
    }

    public open fun isVal() : Boolean {
        return myModifiers.contains(Modifier.FINAL)
    }

    public override fun isStatic() : Boolean {
        return myModifiers.contains(Modifier.STATIC)
    }

    public override fun toKotlin() : String {
        val declaration : String? = modifiersToKotlin() + identifier.toKotlin() + " : " + `type`.toKotlin()
        if (initializer.isEmpty()) {
            return declaration + ((if (isVal() && !isStatic() && writingAccesses == 1)
                ""
            else
                " = " + Converter.getDefaultInitializer(this)))
        }

        return declaration + " = " + initializer.toKotlin()
    }
}
