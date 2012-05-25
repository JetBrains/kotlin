package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.Set

public open class LocalVariable(val identifier: Identifier,
                                val modifiersSet: Set<String>,
                                val `type`: Type,
                                val initializer: Expression): Expression() {

    public open fun hasModifier(modifier: String): Boolean {
        return modifiersSet.contains(modifier)
    }

    public override fun toKotlin(): String {
        if (initializer.isEmpty()) {
            return identifier.toKotlin() + " : " + `type`.toKotlin()
        }

        return identifier.toKotlin() + " : " + `type`.toKotlin() + " = " + initializer.toKotlin()
    }
}
