/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.Comparators
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

public class SirAsSwiftSourcesPrinter(
    private val printer: SmartPrinter,
    private val stableDeclarationsOrder: Boolean,
    private val renderDocComments: Boolean,
    private val emptyBodyStub: SirFunctionBody
) : IndentingPrinter by printer {

    public companion object {

        public val fatalErrorBodyStub: SirFunctionBody = SirFunctionBody(
            listOf("fatalError()")
        )

        public fun print(
            module: SirModule,
            stableDeclarationsOrder: Boolean,
            renderDocComments: Boolean,
            emptyBodyStub: SirFunctionBody = fatalErrorBodyStub
        ): String {
            val childrenPrinter = SirAsSwiftSourcesPrinter(
                SmartPrinter(StringBuilder()),
                stableDeclarationsOrder = stableDeclarationsOrder,
                renderDocComments = renderDocComments,
                emptyBodyStub = emptyBodyStub,
            )
            val declarationsString = with(childrenPrinter) {
                module.printChildren()
                toString().trimIndent()
            }
            val importsString = if (module.imports.isNotEmpty()) {
                // We print imports after module declarations as they might lazily add new imports.
                val importsPrinter = SirAsSwiftSourcesPrinter(
                    SmartPrinter(StringBuilder()),
                    stableDeclarationsOrder = stableDeclarationsOrder,
                    renderDocComments = renderDocComments,
                    emptyBodyStub = emptyBodyStub,
                )
                with(importsPrinter) {
                    module.printImports()
                    println()
                    toString().trimIndent()
                }
            } else ""
            return importsString + declarationsString
        }
    }

    private fun SirModule.printImports() {
        val lastImport = imports.lastOrNull()
        imports.forEach {
            it.print()
            if (it == lastImport) {
                println()
            }
        }
    }

    private fun SirTypealias.print() {
        printDocumentation()
        printVisibility()
        print("typealias ")
        printName()
        print(" = ")
        println(type.swiftName)
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
        if (this is SirClass) {
            printSuperClass()
        }
        println("{")
        withIndent {
            printChildren()
        }
        println("}")
    }

    private fun SirDeclarationContainer.printChildren() {
        allNonPackageEnums()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allTypealiases()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allClasses()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allVariables()
            .sortedWithIfNeeded(Comparators.stableVariableComparator)
            .forEach { it.print() }
        allCallables()
            .sortedWithIfNeeded(Comparators.stableCallableComparator)
            .forEach { it.print() }
        if (this is SirModule) {
            allExtensions()
                .sortedWithIfNeeded(Comparators.stableExtensionComparator)
                .forEach { it.print() }
        }
        allPackageEnums()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
    }

    private inline fun <reified T : SirElement> Sequence<T>.sortedWithIfNeeded(comparator: Comparator<in T>): Sequence<T> =
        if (stableDeclarationsOrder) sortedWith(comparator) else this

    private fun SirVariable.print() {
        printDocumentation()
        printVisibility()
        kind.print()
        print(
            "var ",
            name.swiftIdentifier,
            ": ",
            type.swiftName,
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
        printOverride()
        printPreNameKeywords()
        printName()
        printPostNameKeywords()
        if (this !is SirAccessor) {
            print("(")
        }
        collectParameters().print()
        if (this !is SirAccessor) {
            print(")")
        }
        printReturnType()
        println(" {")
        withIndent {
            body.print()
        }
        println("}")
    }

    private fun SirCallable.printOverride() {
        if (this is SirInit && this.isOverride) {
            print("override ")
        }
    }

    private fun SirDeclaration.printDocumentation() {
        if (!renderDocComments) return
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

    private fun SirClass.printSuperClass() = print(
        superClass?.let { ": ${it.swiftName} " } ?: ""
    )

    private fun SirElement.printName() = print(
        when (this@printName) {
            is SirNamed -> name
            is SirExtension -> extendedType.swiftName
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
            is SirSetter,
            -> ""
        }
    )

    private fun SirCallable.printPostNameKeywords() = when (this) {
        is SirInit -> "?".takeIf { isFailable }?.let { print(it) }
        is SirFunction,
        is SirGetter,
        is SirSetter,
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
            is SirFunction -> " -> ${returnType.swiftName}"
            is SirInit,
            is SirGetter,
            is SirSetter,
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
                        print(sirParameter.swiftName)
                        if (index != lastIndex) {
                            println(",")
                        } else {
                            println()
                        }
                    }
                }
            }

    private fun SirFunctionBody?.print() = (this ?: emptyBodyStub)
        .statements
        .forEach {
            println(it)
        }

    private fun SirCallableKind.print() = print(
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

private val SirParameter.swift get(): String = (argumentName ?: "_") + (parameterName?.let { " $it" } ?: "") + ": " + type.swiftName

private val simpleIdentifierRegex = Regex("[_a-zA-Z][_a-zA-Z0-9]*")

private val String.swiftIdentifier get() = if (simpleIdentifierRegex.matches(this)) this else "`$this`"

private val SirParameter.swiftName
    get(): String = (argumentName ?: "_") +
            (parameterName?.let { " $it" } ?: "") + ": " +
            type.swiftName