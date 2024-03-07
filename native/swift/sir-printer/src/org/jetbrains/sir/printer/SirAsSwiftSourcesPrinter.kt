/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitor
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

public data class RenderContext(
    val currentModule: SirModule?,
)

public class SirAsSwiftSourcesPrinter(private val printer: SmartPrinter) : SirVisitor<Unit, RenderContext>() {

    public constructor() : this(SmartPrinter(StringBuilder()))

    public fun print(element: SirElement): String {
        element.accept(this, RenderContext(currentModule = null))
        return printer.toString().trim()
    }

    override fun visitModule(module: SirModule, data: RenderContext): Unit = with(printer) {

        // We have to write imports before other declarations.
        val (imports, declarations) = module.declarations.partition { it is SirImport }

        imports.forEach {
            it.accept(this@SirAsSwiftSourcesPrinter, RenderContext(currentModule = module))
        }
        if (imports.isNotEmpty()) {
            println()
        }
        declarations.forEach {
            it.accept(this@SirAsSwiftSourcesPrinter, RenderContext(currentModule = module))
            if (module.declarations.last() != it) {
                println()
            }
        }
    }

    override fun visitImport(import: SirImport, data: RenderContext): Unit = with(printer) {
        println("import ${import.moduleName}")
    }

    override fun visitClass(klass: SirClass, data: RenderContext): Unit = with(printer) {
        printVisibility(klass)
        print(
            "class ",
            klass.name.swiftIdentifier,
        )
        klass.superClass?.let {
            print(
                " : ",
                with(data) { renderType(it) }
            )
        }
        println(" {")
        withIndent {
            klass.acceptChildren(this@SirAsSwiftSourcesPrinter, data)
        }
        println("}")
    }

    override fun visitVariable(variable: SirVariable, data: RenderContext): Unit = with(printer) {
        printVisibility(variable)
        printCallableKind(variable.kind)
        print(
            "var ",
            variable.name.swiftIdentifier,
            ": ",
            with(data) { renderType(variable.type) },
        )
        println(" {")
        withIndent {
            variable.getter.accept(this@SirAsSwiftSourcesPrinter, data)
            variable.setter?.accept(this@SirAsSwiftSourcesPrinter, data)
        }
        println("}")
    }

    override fun visitSetter(setter: SirSetter, data: RenderContext): Unit = with(printer) {
        print("set")
        setter.body?.let { body ->
            println(" {")
            withIndent {
                printFunctionBody(body).forEach { println(it) }
            }
            println("}")
        } ?: println("")
    }

    override fun visitGetter(getter: SirGetter, data: RenderContext): Unit = with(printer) {
        print("get")
        getter.body?.let { body ->
            println(" {")
            withIndent {
                printFunctionBody(body).forEach { println(it) }
            }
            println("}")
        } ?: println("")
    }

    override fun visitFunction(function: SirFunction, data: RenderContext): Unit = with(printer) {
        function.documentation?.let { println(it) }
        printVisibility(function)
        printCallableKind(function.kind)
        print(
            "func ",
            function.name.swiftIdentifier,
            "("
        )
        if (function.parameters.isNotEmpty()) {
            println()
            withIndent {
                function.parameters.forEachIndexed { index, sirParameter ->
                    print(with(data) { renderParameter(sirParameter) })
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
            with(data) { renderType(function.returnType) },
        )
        println(" {")
        withIndent {
            printFunctionBody(function.body).forEach {
                println(it)
            }
        }
        println("}")
    }

    override fun visitEnum(enum: SirEnum, data: RenderContext): Unit = with(printer) {
        printVisibility(enum)
        println(
            "enum ",
            enum.name.swiftIdentifier,
            " {"
        )
        withIndent {
            enum.acceptChildren(this@SirAsSwiftSourcesPrinter, data)
        }
        println("}")
    }

    override fun visitElement(element: SirElement, data: RenderContext): Unit = with(printer) {
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

context(RenderContext)
private fun renderParameter(parameter: SirParameter) = (parameter.argumentName) + ": " + renderType(parameter.type)

context(RenderContext)
private fun renderType(sirType: SirType): String {
    return when (sirType) {
        is SirExistentialType -> "Any"
        is SirNominalType -> renderDeclarationReference(sirType.type)
    }
}

context(RenderContext)
private fun renderDeclarationReference(declaration: SirNamedDeclaration): String {
    val parentName = when (val parent = declaration.parent) {
        is SirNamedDeclaration -> renderDeclarationReference(parent)
        is SirModule -> if (parent == currentModule) null else parent.name
        is SirNamed -> parent.name
        else -> null
    }
    return parentName?.let { "$it.${declaration.name}" } ?: declaration.name
}

private val simpleIdentifierRegex = Regex("[_a-zA-Z][_a-zA-Z0-9]*")

private val String.swiftIdentifier get() = if (simpleIdentifierRegex.matches(this)) this else "`$this`"

internal fun SmartPrinter.printVisibility(decl: SirDeclaration) {
    print(
        decl.visibility.takeIf { it != SirVisibility.INTERNAL }?.let { "${it.swift} " } ?: ""
    )
}

internal fun SmartPrinter.printCallableKind(callableKind: SirCallableKind) {
    print(
        when (callableKind) {
            SirCallableKind.FUNCTION -> ""
            SirCallableKind.INSTANCE_METHOD -> ""
            SirCallableKind.CLASS_METHOD -> "class "
            SirCallableKind.STATIC_METHOD -> "static "
        }
    )
}
