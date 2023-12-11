/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.kotlin.utils.SmartPrinter

private const val DEFAULT_INDENT: String = "    "

public class SirAsSwiftSourcesPrinter(private val printer: SmartPrinter) : SirVisitorVoid() {

    public constructor() : this(SmartPrinter(StringBuilder()))

    public fun print(element: SirElement): String {
        element.accept(this)
        return printer.toString().trim()
    }

    override fun visitModule(module: SirModule): Unit = with(printer) {
        module.declarations.forEach {
            it.accept(this@SirAsSwiftSourcesPrinter)
            if (module.declarations.last() != it) {
                println()
            }
        }
    }

    override fun visitFunction(function: SirFunction): Unit = with(printer) {
        println(
            listOfNotNull(
                function.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " },
                "func ",
                function.name.swiftIdentifier,
                function.parameters.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "(\n", postfix = "\n)", separator = ",\n") {
                        it.swift.prependIndent(DEFAULT_INDENT)
                    } ?: "()",
                " -> ",
                function.returnType.swift,
                " { fatalError() }",
            ).joinToString(separator = ""),
        )
    }

    override fun visitForeignFunction(function: SirForeignFunction) {} // we do not write foreign nodes

    override fun visitElement(element: SirElement): Unit = with(printer) {
        println("/* ERROR: unsupported element type: " + element.javaClass.simpleName + " */")
    }
}

private val SirVisibility.swift
    get(): String = when (this) {
        SirVisibility.PRIVATE -> "private"
        SirVisibility.FILEPRIVATE -> "fileprivate"
        SirVisibility.INTERNAL -> "internal"
        SirVisibility.PUBLIC -> "public"
        SirVisibility.PACKAGE -> "package"
    }

private val SirParameter.swift get(): String = (argumentName ?: "_") + (parameterName?.let { " $it" } ?: "") + ": " + type.swift

private val SirType.swift
    get(): String = when (this) {
        is SirExistentialType -> "Any"
        is SirNominalType -> type.swiftFqName
    }

private val SirNamedDeclaration.swiftFqName: String
    get() {
        val parentName = (parent as? SirNamedDeclaration)?.swiftFqName ?: ((parent as? SirNamed)?.name)
        return parentName?.let { "$it.$name" } ?: name
    }

private val simpleIdentifierRegex = Regex("[_a-zA-Z][_a-zA-Z0-9]*")

private val String.swiftIdentifier get() = if (simpleIdentifierRegex.matches(this)) this else "`$this`"