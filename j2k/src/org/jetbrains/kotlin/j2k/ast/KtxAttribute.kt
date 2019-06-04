package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder

class KtxAttribute(
    val key: Identifier,
    val value: Expression
) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        with(builder) {
            append(key)
            append("=")
            append(value)
        }
    }
}