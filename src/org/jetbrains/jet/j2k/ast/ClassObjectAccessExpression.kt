package org.jetbrains.jet.j2k.ast


public open class ClassObjectAccessExpression(val typeElement: Element): Expression() {
    public override fun toKotlin(): String {
        return "getJavaClass<" + typeElement.toKotlin() + ">"
    }
}
