package org.jetbrains.jet.j2k.ast

public open class ArrayAccessExpression(val expression : Expression, val index : Expression) : Expression() {
    override fun toKotlin() = expression.toKotlin() + "[" + index.toKotlin() + "]"
    override fun isNullable() = expression.isNullable()
}

public open class AssignmentExpression(val left : Expression, val right : Expression, val op : String) : Expression() {
    override fun toKotlin() = left.toKotlin() + " "+ op + " "+ right.toKotlin()
}

public class BangBangExpression(val expr: Expression): Expression() {
    override fun toKotlin() = expr.toKotlin() + "!!"
}

public open class BinaryExpression(val left: Expression, val right: Expression, val op: String): Expression() {
    override fun toKotlin() = left.toKotlin() + " " + op + " " + right.toKotlin()
}

public open class ClassObjectAccessExpression(val typeElement: Element): Expression() {
    override fun toKotlin() = "getJavaClass<" + typeElement.toKotlin() + ">"
}

public open class IsOperator(val expression: Expression, val typeElement: TypeElement): Expression() {
    override fun toKotlin() = expression.toKotlin() + " is " + typeElement.`type`.convertedToNotNull().toKotlin()
}

public open class LiteralExpression(val literalText: String): Expression() {
    override fun toKotlin() = literalText
}
