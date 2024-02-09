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

    override fun visitModule(module: SirModule, data: Unit): String = render(
        module,
        listOf(
            "name" to module.name
        ),
        module.declarations
    )

    override fun visitEnum(enum: SirEnum, data: Unit): String = render(
        enum,
        listOf(
            "origin" to enum.origin,
            "visibility" to enum.visibility,
            "name" to enum.name,
            "cases" to enum.cases
        ),
        enum.declarations
    )

    override fun visitStruct(struct: SirStruct, data: Unit): String = render(
        struct,
        listOf(
            "origin" to struct.origin,
            "visibility" to struct.visibility,
            "name" to struct.name
        ),
        struct.declarations
    )

    override fun visitFunction(function: SirFunction, data: Unit): String = render(
        function,
        listOf(
            "origin" to function.origin,
            "visibility" to function.visibility,
            "name" to function.name,
            "parameters" to function.parameters,
            "returnType" to function.returnType,
        )
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