package org.jetbrains.uast.internal

import com.intellij.psi.PsiElement
import org.jetbrains.uast.LINE_SEPARATOR
import org.jetbrains.uast.UElement
import org.jetbrains.uast.asLogString
import org.jetbrains.uast.visitor.UastVisitor
import org.jetbrains.uast.withMargin

/**
 * Builds the log message for the [UElement.asLogString] function.
 *
 * @param firstLine the message line (the interface name, some optional information).
 * @param nested nested UElements. Could be `List<UElement>`, [UElement] or `null`.
 * @throws IllegalStateException if the [nested] argument is invalid.
 * @return the rendered log string.
 */
fun UElement.log(firstLine: String, vararg nested: Any?): String {
    return (if (firstLine.isBlank()) "" else firstLine + LINE_SEPARATOR) + nested.joinToString(LINE_SEPARATOR) {
        when (it) {
            null -> "<no element>".withMargin
            is List<*> -> {
                if (it.firstOrNull() is PsiElement) {
                    @Suppress("UNCHECKED_CAST")
                    (it as List<PsiElement>).joinToString(LINE_SEPARATOR) { it.text }
                } else {
                    @Suppress("UNCHECKED_CAST")
                    (it as List<UElement>).asLogString()
                }
            }
            is UElement -> it.asLogString().withMargin
            else -> error("Invalid element type: $it")
        }
    }
}

fun List<UElement>.acceptList(visitor: UastVisitor) {
    for (element in this) {
        element.accept(visitor)
    }
}