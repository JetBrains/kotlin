package org.jetbrains.jet.j2k.ast

import java.util.LinkedList

public open class Block(val statements: List<Element>, val notEmpty: Boolean = false): Statement() {
    public override fun isEmpty(): Boolean {
        return !notEmpty && statements.size() == 0
    }

    public override fun toKotlin(): String {
        if (!isEmpty()) {
            return "{\n" + statements.toKotlin("\n") + "\n}"
        }

        return ""
    }

    class object {
        public val EMPTY_BLOCK: Block = Block(arrayList())
    }
}
