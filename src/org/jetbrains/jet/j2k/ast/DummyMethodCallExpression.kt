package org.jetbrains.jet.j2k.ast


public open class DummyMethodCallExpression(val who: Element, val methodName: String, val what: Element): Expression() {
    public override fun toKotlin(): String {
        return who.toKotlin() + "." + methodName + "(" + what.toKotlin() + ")"
    }
}
