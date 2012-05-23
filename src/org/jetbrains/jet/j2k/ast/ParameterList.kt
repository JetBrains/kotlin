package org.jetbrains.jet.j2k.ast

import java.util.List

public open class ParameterList(val parameters : List<Parameter>) : Expression() {
    public override fun toKotlin() = parameters.map { it.toKotlin() }.makeString(", ")
}

