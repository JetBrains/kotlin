package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.Collections
import java.util.List

public open class AnonymousClass(converter : Converter, members : List<Member>)
                                 : Class(converter,
                                         IdentifierImpl("anonClass"),
                                         Collections.emptySet<String>()!!,
                                         Collections.emptyList<Element>()!!,
                                         Collections.emptyList<Type>()!!,
                                         Collections.emptyList<Expression>()!!,
                                         Collections.emptyList<Type>()!!, members) {
    public override fun toKotlin() = bodyToKotlin()
}
