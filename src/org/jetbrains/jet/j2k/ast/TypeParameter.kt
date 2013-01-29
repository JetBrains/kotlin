package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class TypeParameter(val name : Identifier, val extendsTypes : List<Type>) : Element() {
    public open fun hasWhere() : Boolean = extendsTypes.size() > 1
    public open fun getWhereToKotlin() : String {
        if (hasWhere()) {
            return name.toKotlin() + " : " + extendsTypes.get(1).toKotlin()
        }

        return ""
    }

    public override fun toKotlin() : String {
        if (extendsTypes.size() > 0) {
            return name.toKotlin() + " : " + extendsTypes [0].toKotlin()
        }

        return name.toKotlin()
    }
}
