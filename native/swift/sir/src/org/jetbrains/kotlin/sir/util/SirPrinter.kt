/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitor

object SirPrinter : SirVisitor<String, Unit>() {
    fun toString(element: SirElement): String = element.accept(this, Unit)

    override fun visitElement(element: SirElement, data: Unit): String {
        return "UNKNOWN<${SirElement::class.simpleName}>($element)"
    }

    override fun visitModule(element: SirModule, data: Unit): String = render(
        element,
        listOf(
            "name" to element.name
        ),
        element.declarations
    )

    override fun visitEnum(element: SirEnum, data: Unit): String = render(
        element,
        listOf(
            "origin" to element.origin,
            "visibility" to element.visibility,
            "name" to element.name,
            "cases" to element.cases
        ),
        element.declarations
    )

    override fun visitStruct(element: SirStruct, data: Unit): String = render(
        element,
        listOf(
            "origin" to element.origin,
            "visibility" to element.visibility,
            "name" to element.name
        ),
        element.declarations
    )

    override fun visitFunction(element: SirFunction, data: Unit): String = render(
        element,
        listOf(
            "origin" to element.origin,
            "visibility" to element.visibility,
            "name" to element.name,
            "parameters" to element.parameters,
            "returnType" to element.returnType,
        )
    )

    override fun visitForeignFunction(element: SirForeignFunction, data: Unit): String = render(
        element,
        listOf(
            "origin" to element.origin,
            "visibility" to element.visibility,
        ),
    )
}

private fun render(
    base: SirElement,
    attributes: List<Pair<String, Any?>> = emptyList(),
    children: List<SirElement> = emptyList(),
): String {
    return "${base::class.simpleName}${attributes.renderAsAttributres()} ${children.renderAsChildren()}"
}

private fun List<SirElement>.renderAsChildren(): String {
    return this.takeIf { isNotEmpty() }?.joinToString(prefix = "{\n", separator = "\n", postfix = "\n}") {
        SirPrinter.toString(it).prependIndent("  ")
    } ?: "{}"
}

private fun List<Pair<String, Any?>>.renderAsAttributres(): String {
    return this.takeIf { isNotEmpty() }?.joinToString(prefix = "(\n", separator = "\n", postfix = "\n)") {
        "  ${it.first}: ${it.second}"
    } ?: "()"
}