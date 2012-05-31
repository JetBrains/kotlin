package org.jetbrains.jet.j2k.ast


public abstract class Expression(): Statement() {
    public open fun isNullable(): Boolean {
        return false
    }

    class object {
        public val EMPTY_EXPRESSION: Expression = object: Expression() {
            public override fun toKotlin()= ""
            public override fun isEmpty() = true
        }
    }
}
