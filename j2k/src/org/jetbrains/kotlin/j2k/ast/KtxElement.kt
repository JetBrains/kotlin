package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append

class KtxElement(
    val tag: Identifier,
    val attributes: List<KtxAttribute>,
    val body: List<Element>
) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        with(builder) {
            append("<")
            append(tag)
            if (attributes.isNotEmpty()) {
                append(attributes, separator = "\n", prefix = "\n")
            }

            if (body.isEmpty()) {
                append("/>")
            } else {
                append(">")
                append(body, "\n", "\n", "\n")
                append("</")
                append(tag)
                append(">")
            }
        }
    }
}