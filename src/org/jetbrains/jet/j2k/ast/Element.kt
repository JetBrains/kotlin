package org.jetbrains.jet.j2k.ast


public abstract class Element(): Node() {
    public open fun isEmpty(): Boolean {
        return false
    }

    class object {
        public val EMPTY_ELEMENT: Element = object : Element() {
            override fun toKotlin() = ""
            override fun isEmpty() = true
        }
    }
}
