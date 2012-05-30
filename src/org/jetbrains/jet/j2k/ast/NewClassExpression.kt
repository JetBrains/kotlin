package org.jetbrains.jet.j2k.ast

import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.j2k.util.AstUtil
import java.util.List

public open class NewClassExpression(val name: Element,
                                     val arguments: List<Expression>,
                                     val qualifier: Expression = Expression.EMPTY_EXPRESSION,
                                     val anonymousClass: AnonymousClass? = null): Expression() {
    public override fun toKotlin(): String {
        val callOperator: String? = (if (qualifier.isNullable().sure())
            "?."
        else
            ".")
        val qualifier: String? = (if (qualifier.isEmpty().sure())
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
