/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

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
        print(
            function.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " } ?: "",
            "func ",
            function.name.swiftIdentifier,
            "("
        )
        if (function.parameters.isNotEmpty()) {
            println()
            withIndent {
                function.parameters.forEachIndexed { index, sirParameter ->
                    print(sirParameter.swift)
                    if (index != function.parameters.lastIndex) {
                        println(",")
                    } else {
                        println()
                    }
                }
            }
        }
        print(
            ")",
            " -> ",
            function.returnType.swift,
        )
        println(" {")
        withIndent {
            printFunctionBody(function).forEach {
                println(it)
            }
        }
        println("}")
    }

    override fun visitEnum(enum: SirEnum): Unit = with(printer) {
        println("enum ${enum.name.swiftIdentifier} {")
        withIndent {
            enum.acceptChildren(this@SirAsSwiftSourcesPrinter)
        }
        println("}")
    }

    override fun visitForeignFunction(function: SirForeignFunction) {} // we do not write foreign nodes

    override fun visitElement(element: SirElement): Unit = with(printer) {
        println("/* ERROR: unsupported element type: " + element.javaClass.simpleName + " */")
    }
}

private fun printFunctionBody(function: SirFunction): List<String> {
    return listOf("fatalError()")
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