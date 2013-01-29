package org.jetbrains.jet.j2k.ast

import org.jetbrains.annotations.Nullable

public open class NewClassExpression(val name: Element,
                                     val arguments: List<Expression>,
                                     val qualifier: Expression = Expression.EMPTY_EXPRESSION,
                                     val anonymousClass: AnonymousClass? = null): Expression() {
    public override fun toKotlin(): String {
        val callOperator: String? = (if (qualifier.isNullable()!!)
            "?."
        else
            ".")
        val qualifier: String? = (if (qualifier.isEmpty()!!)
            ""
        else
            qualifier.toKotlin() + callOperator)
        val appliedArguments: String = arguments.toKotlin(", ")
        return (if (anonymousClass != null)
            "object : " + qualifier + name.toKotlin() + "(" + appliedArguments + ")" + anonymousClass.toKotlin()
        else
            qualifier + name.toKotlin() + "(" + appliedArguments + ")")
    }
}
