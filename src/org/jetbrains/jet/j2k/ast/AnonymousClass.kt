package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.Collections

public open class AnonymousClass(converter : Converter, members : List<Node>)
                                 : Class(converter,
                                         Identifier("anonClass"),
                                         arrayList(),
                                         Collections.emptySet<Modifier>()!!,
                                         Collections.emptyList<Element>()!!,
                                         Collections.emptyList<Type>()!!,
                                         Collections.emptyList<Expression>()!!,
                                         Collections.emptyList<Type>()!!, members) {
    public override fun toKotlin() = bodyToKotlin()
}
