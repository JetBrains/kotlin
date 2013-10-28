package org.jetbrains.jet.j2k.ast


public open class AssertStatement(val condition : Expression, val detail : Expression) : Statement() {
    public override fun toKotlin() : String {
        var detail : String? = (if (detail != Expression.EMPTY_EXPRESSION)
            "(" + detail.toKotlin() + ")"
        else
            "")
        return "assert" + detail + " {" + condition.toKotlin() + "}"
    }
}
