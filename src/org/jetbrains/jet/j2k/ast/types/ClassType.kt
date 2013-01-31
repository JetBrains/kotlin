package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.Element
import org.jetbrains.jet.j2k.ast.Identifier
import java.util.Collections
import java.util.ArrayList

public open class ClassType(val `type` : Identifier, val parameters : List<Element>, nullable : Boolean) : Type(nullable) {
    public override fun toKotlin() : String {
        // TODO change to map() when KT-2051 is fixed
        val parametersToKotlin = ArrayList<String>()
        for(val param in parameters) {
            parametersToKotlin.add(param.toKotlin())
        }
        var params : String = if (parametersToKotlin.size() == 0)
            ""
        else
            "<" + parametersToKotlin.makeString(", ") + ">"
        return `type`.toKotlin() + params + isNullableStr()
    }


    public override fun convertedToNotNull() : Type = ClassType(`type`, parameters, false)
}
