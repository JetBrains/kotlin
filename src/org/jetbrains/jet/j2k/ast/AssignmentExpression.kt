package org.jetbrains.jet.j2k.ast


public open class AssignmentExpression(val left : Expression, val right : Expression, val op : String) : Expression() {
    public override fun toKotlin() : String = left.toKotlin() + " "+ op + " "+ right.toKotlin()
}
