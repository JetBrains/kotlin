/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

public class SirAsSwiftSourcesPrinter(
    private val printer: SmartPrinter,
) : IndentingPrinter by printer {

    public companion object {
        public fun print(module: SirModule): String {
            val printer = SirAsSwiftSourcesPrinter(SmartPrinter(StringBuilder()))
            with(printer) { module.print() }
            return printer.toString().trimIndent()
        }
    }

    public fun SirModule.print() {
        printImports()
        printChildren()
    }

    private fun SirModule.printImports() {
        val imports = allImports()
        val lastImport = imports.lastOrNull()
        imports.forEach {
            it.print()
            if (it == lastImport) {
                println()
            }
        }
    }

    private fun SirModule.printExtensions() {
        allExtensions().forEach {
            it.print()
        }
    }

    private fun SirDeclarationContainer.print() {
        (this as? SirDeclaration)?.let {
            printDocumentation()
            printVisibility()
        }

        printContainerKeyword()
        print(" ")
        printName()
        print(" ")
        println("{")
        withIndent {
            printChildren()
        }
        println("}")
    }

    private fun SirDeclarationContainer.printChildren() {
        allNonPackageEnums().forEach {
            it.print()
        }
        allClasses().forEach {
            it.print()
        }
        allVariables().forEach {
            it.print()
        }
        allCallables().forEach {
            it.print()
        }
        (this as? SirModule)?.printExtensions()
        allPackageEnums().forEach {
            it.print()
        }
    }

    private fun SirVariable.print() {
        printDocumentation()
        printVisibility()
        kind.print()
        print(
            "var ",
            name.swiftIdentifier,
            ": ",
            type.swift,
        )
        println(" {")
        withIndent {
            getter.print()
            setter?.print()
        }
        println("}")
    }

    private fun SirCallable.print() {
        printDocumentation()
        printVisibility()
        printPreNameKeywords()
        printName()
        printPostNameKeywords()
        if (this !is SirAccessor) { print("(") }
        collectParameters().print()
        if (this !is SirAccessor) { print(")") }
        printReturnType()
        println(" {")
        withIndent {
            body.print()
        }
        println("}")
    }

    private fun SirDeclaration.printDocumentation() {
        documentation?.lines()?.forEach { println(it.trimIndent()) }
    }

    private fun SirImport.print() = println("import $moduleName")

    private fun SirDeclarationContainer.printContainerKeyword() = print(
        when (this@printContainerKeyword) {
            is SirClass -> "class"
            is SirEnum -> "enum"
            is SirExtension -> "extension"
            is SirStruct -> "struct"
            is SirModule -> error("there is no keyword for module. Do not print module as declaration container.")
        }
    )

    private fun SirElement.printName() =print(
        when (this@printName) {
            is SirNamed -> name
            is SirExtension -> extendedType.swift
            else -> error("There is no printable name for SirElement: ${this@printName}")
        }
    )

    private fun SirDeclaration.printVisibility() = print(
        visibility
            .takeUnless { this is SirAccessor }
            .takeIf { it != SirVisibility.INTERNAL }
            ?.let { "${it.swift} " }
            ?: ""
    )

    private fun SirCallable.printPreNameKeywords() = when (this) {
        is SirInit -> initKind.print()
        is SirFunction -> kind.print()
        is SirGetter -> print("get")
        is SirSetter -> print("set")
    }

    private fun SirCallable.printName() = print(
        when (this) {
            is SirInit -> "init"
            is SirFunction -> "func $name"
            is SirGetter,
            is SirSetter
            -> ""
        }
    )

    private fun SirCallable.printPostNameKeywords() = when (this) {
        is SirInit -> "?".takeIf { isFailable }?.let { print(it) }
        is SirFunction,
        is SirGetter,
        is SirSetter
        -> print("")
    }

    private fun SirCallable.collectParameters(): List<SirParameter> = when (this) {
        is SirGetter -> emptyList()
        is SirSetter -> emptyList()
        is SirFunction -> parameters
        is SirInit -> parameters
    }

    private fun SirCallable.printReturnType() = print(
        when (this) {
            is SirFunction -> " -> ${returnType.swift}"
            is SirInit,
            is SirGetter,
            is SirSetter
            -> ""
        }
    )

    private fun SirInitializerKind.print() = print(
        when (this) {
            SirInitializerKind.ORDINARY -> ""
            SirInitializerKind.REQUIRED -> "required "
            SirInitializerKind.CONVENIENCE -> "convenience "
        }
    )

    private fun List<SirParameter>.print() =
        takeIf { it.isNotEmpty() }
            ?.let {
                println()
                withIndent {
                    this.forEachIndexed { index, sirParameter ->
                        print(sirParameter.swift)
                        if (index != lastIndex) {
                            println(",")
                        } else {
                            println()
                        }
                    }
                }
            }

    private fun SirFunctionBody?.print() = (this?.statements ?: listOf("fatalError()"))
        .forEach {
            println(it)
        }

    private fun SirCallableKind.print() =print(
        when (this) {
            SirCallableKind.FUNCTION -> ""
            SirCallableKind.INSTANCE_METHOD -> ""
            SirCallableKind.CLASS_METHOD -> "class "
            SirCallableKind.STATIC_METHOD -> "static "
        }
    )
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
