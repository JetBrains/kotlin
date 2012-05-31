package org.jetbrains.jet.j2k.ast

import java.util.LinkedList
import java.util.List

public open class Block(val statements: List<Statement>, val notEmpty: Boolean = false): Statement() {
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
