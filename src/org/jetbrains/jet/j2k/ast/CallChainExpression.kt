package org.jetbrains.jet.j2k.ast

import a.h.id


public open class CallChainExpression(val expression : Expression, val identifier : Expression) : Expression() {
    public override fun getKind() : INode.Kind {
        return INode.Kind.CALL_CHAIN
    }

    public override fun isNullable() : Boolean {
        if (!expression.isEmpty() && expression.isNullable()) return true
        return identifier.isNullable()
    }

    public override fun toKotlin() : String {
        if (!expression.isEmpty()) {
            return expression.toKotlin() + (if (expression.isNullable()) "?." else ".") + identifier.toKotlin()
        }

        return identifier.toKotlin()
    }
}
