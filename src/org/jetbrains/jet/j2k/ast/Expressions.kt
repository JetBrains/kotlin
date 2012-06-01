package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

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

public open class ClassObjectAccessExpression(val typeElement: TypeElement): Expression() {
    override fun toKotlin() = "javaClass<" + typeElement.toKotlinNotNull() + ">()"
}

public open class IsOperator(val expression: Expression, val typeElement: TypeElement): Expression() {
    override fun toKotlin() = expression.toKotlin() + " is " + typeElement.toKotlinNotNull()
}

public open class TypeCastExpression(val `type` : Type, val expression : Expression) : Expression() {
    override fun toKotlin() = "(" + expression.toKotlin() + " as " + `type`.toKotlin() + ")"
}

public open class LiteralExpression(val literalText: String): Expression() {
    override fun toKotlin() = literalText
}

public open class ParenthesizedExpression(val expression : Expression) : Expression() {
    override fun toKotlin() = "(" + expression.toKotlin() + ")"
}

public open class PrefixOperator(val op: String, val expression: Expression): Expression() {
    override fun toKotlin() = op + expression.toKotlin()
    override fun isNullable() = expression.isNullable()
}

public open class PostfixOperator(val op: String, val expression: Expression): Expression() {
    override fun toKotlin() = expression.toKotlin() + op
}

public open class ThisExpression(val identifier: Identifier) : Expression() {
    override fun toKotlin() = "this" + identifier.withPrefix("@")
}

public open class SuperExpression(val identifier : Identifier) : Expression() {
    override fun toKotlin() = "super" + identifier.withPrefix("@")
}
