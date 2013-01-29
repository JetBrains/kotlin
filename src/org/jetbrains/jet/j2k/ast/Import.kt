package org.jetbrains.jet.j2k.ast


public open class Import(val name: String): Node() {
    public override fun toKotlin() = "import " + name
}
