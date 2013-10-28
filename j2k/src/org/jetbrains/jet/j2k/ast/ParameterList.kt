package org.jetbrains.jet.j2k.ast


public open class ParameterList(val parameters : List<Parameter>) : Expression() {
    public override fun toKotlin() = parameters.map { it.toKotlin() }.makeString(", ")
}

