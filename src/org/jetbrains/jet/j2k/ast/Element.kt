package org.jetbrains.jet.j2k.ast


public abstract class Element(): Node() {
    public open fun isEmpty(): Boolean = false

    class object {
        public val EMPTY_ELEMENT: Element = object : Element() {
            override fun toKotlin() = ""
            override fun isEmpty() = true
        }
    }
}

public class Comment(val text: String): Element() {
    override fun toKotlin() = text
}
