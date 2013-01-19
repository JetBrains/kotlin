package org.jetbrains.jet.j2k.ast


public abstract class Node() {
    public abstract fun toKotlin(): String

    class object {
        public val PRIMITIVE_TYPES: Set<String> = hashSet(
                "double", "float", "long", "int", "short", "byte", "boolean", "char")
    }
}
