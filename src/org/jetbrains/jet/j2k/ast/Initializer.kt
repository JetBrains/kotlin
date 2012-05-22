package org.jetbrains.jet.j2k.ast

import java.util.Set

public open class Initializer(val block : Block, modifiers : Set<String?>) : Member(modifiers) {
    public override fun toKotlin() : String {
        return block.toKotlin()
    }
}
