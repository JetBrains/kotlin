package org.jetbrains.jet.j2k.ast


public open class CatchStatement(val variable: Parameter, val block: Block): Statement() {
    public override fun toKotlin(): String {
        return "catch (" + variable.toKotlin() + ") " + block.toKotlin()
    }
}
