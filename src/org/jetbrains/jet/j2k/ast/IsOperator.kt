package org.jetbrains.jet.j2k.ast


public open class IsOperator(val expression: Expression, val typeElement: TypeElement): Expression() {
    public override fun toKotlin(): String {
        return expression.toKotlin() + " is " + typeElement.`type`.convertedToNotNull().toKotlin()
    }
}
