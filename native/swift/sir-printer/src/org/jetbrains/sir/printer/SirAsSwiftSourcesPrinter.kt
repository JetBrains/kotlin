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
        // We have to write imports before other declarations.
        val (imports, declarations) = module.declarations.partition { it is SirImport }

        imports.forEach {
            it.accept(this@SirAsSwiftSourcesPrinter)
        }
        if (imports.isNotEmpty()) {
            println()
        }
        declarations.forEach {
            it.accept(this@SirAsSwiftSourcesPrinter)
            if (module.declarations.last() != it) {
                println()
            }
        }
    }

    override fun visitImport(import: SirImport): Unit = with(printer) {
        println("import ${import.moduleName}")
    }

    override fun visitVariable(variable: SirVariable): Unit = with(printer) {
        print(
            variable.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " } ?: "",
            if (variable.isStatic) "static " else "",
            "var ",
            variable.name.swiftIdentifier,
            ": ",
            variable.type.swift,
        )
        println(" {")
        withIndent {
            variable.getter.accept(this@SirAsSwiftSourcesPrinter)
            variable.setter?.accept(this@SirAsSwiftSourcesPrinter)
        }
        println("}")
    }

    override fun visitSetter(setter: SirSetter): Unit = with(printer) {
        print("set")
        setter.body?.let { body ->
            println(" {")
            withIndent {
                printFunctionBody(body).forEach { println(it) }
            }
            println("}")
        } ?: println("")
    }

    override fun visitGetter(getter: SirGetter): Unit = with(printer) {
        print("get")
        getter.body?.let { body ->
            println(" {")
            withIndent {
                printFunctionBody(body).forEach { println(it) }
            }
            println("}")
        } ?: println("")
    }

    override fun visitFunction(function: SirFunction): Unit = with(printer) {
        function.documentation?.let { println(it) }
        print(
            function.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " } ?: "",
            if (function.isStatic) {
                "static "
            } else {
                ""
            },
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
            printFunctionBody(function.body).forEach {
                println(it)
            }
        }
        println("}")
    }

    override fun visitEnum(enum: SirEnum): Unit = with(printer) {
        println(
            enum.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " } ?: "",
            "enum ",
            enum.name.swiftIdentifier,
            " {"
        )
        withIndent {
            enum.acceptChildren(this@SirAsSwiftSourcesPrinter)
        }
        println("}")
    }

    override fun visitElement(element: SirElement): Unit = with(printer) {
        println("/* ERROR: unsupported element type: " + element.javaClass.simpleName + " */")
    }
}

private fun printFunctionBody(body: SirFunctionBody?): List<String> {
    return body?.statements ?: listOf("fatalError()")
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