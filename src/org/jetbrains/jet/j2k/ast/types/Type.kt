package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.Element
import org.jetbrains.jet.j2k.ast.INode

public abstract class Type(val nullable: Boolean) : Element() {
    public open fun convertedToNotNull() : Type {
        if (nullable) throw UnsupportedOperationException("convertedToNotNull must be defined")
        return this
    }

    public open fun isNullableStr() : String? {
        return (if (nullable)
            "?"
        else
            "")
    }
}
