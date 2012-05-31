package org.jetbrains.jet.j2k.ast

import org.eclipse.jdt.internal.core.search.StringOperation
import java.util.Arrays
import java.util.HashSet
import java.util.Set

public abstract class Node() {
    public abstract fun toKotlin(): String

    class object {
        public val PRIMITIVE_TYPES: Set<String> = hashSet(
                "double", "float", "long", "int", "short", "byte", "boolean", "char")
    }
}
