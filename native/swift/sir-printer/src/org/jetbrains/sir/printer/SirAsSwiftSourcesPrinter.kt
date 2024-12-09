/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

private data class Context(val declaration: SirDeclarationParent)

public class SirAsSwiftSourcesPrinter private constructor(
    private val printer: ContextualizedPrinter<Context>,
    private val stableDeclarationsOrder: Boolean,
    private val renderDocComments: Boolean,
    private val emptyBodyStub: SirFunctionBody
) : ContextualizedPrinter<Context> by printer {
    public companion object {

        private val fatalErrorBodyStub: SirFunctionBody = SirFunctionBody(
            listOf("fatalError()")
        )

        public fun print(
            module: SirModule,
            stableDeclarationsOrder: Boolean,
            renderDocComments: Boolean,
            emptyBodyStub: SirFunctionBody = fatalErrorBodyStub
        ): String {
            val childrenPrinter = SirAsSwiftSourcesPrinter(
                ContextualizedPrinterImpl(SmartPrinter(StringBuilder()), Context(module)),
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
                    ContextualizedPrinterImpl(SmartPrinter(StringBuilder()), Context(module)),
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
        imports
            .let {
                if (stableDeclarationsOrder)
                    imports.sortedWith(compareBy(
                            { it.moduleName },
                            { it.mode },
                        ))
                else
                    imports
            }.takeIf {
                it.isNotEmpty()
            }?.forEach {
                it.print()
            }?.also {
                println()
            }
    }

    private fun SirDeclaration.print() {
        printDocumentation()
        printAttributes()

        when (this) {
            is SirClass -> printDeclaration()
            is SirEnum -> printDeclaration()
            is SirExtension -> printDeclaration()
            is SirStruct -> printDeclaration()
            is SirProtocol -> printDeclaration()
            is SirCallable -> printDeclaration()
            is SirVariable -> printDeclaration()
            is SirTypealias -> printDeclaration()
        }
    }

    private fun SirTypealias.printDeclaration() {
        printVisibility()
        print("typealias ")
        printName()
        print(" = ")
        println(type.swiftRender)
    }

    private fun SirClass.printDeclaration() {
        printModifiers()
        print("class ")
        printName()
        printInheritanceClause()
        printBody()
    }

    private fun SirEnum.printDeclaration() {
        printVisibility()
        print("enum ")
        printName()
        printInheritanceClause()
        printBody()
    }

    private fun SirStruct.printDeclaration() {
        printVisibility()
        print("struct ")
        printName()
        printInheritanceClause()
        printBody()
    }

    private fun SirExtension.printDeclaration() {
        printVisibility()
        print("extension ")
        printName()
        printInheritanceClause()
        printBody()
    }

    private fun SirProtocol.printDeclaration() {
        printVisibility()
        print("protocol ")
        printName()
        printInheritanceClause()
        printBody()
    }

    private fun SirDeclarationContainer.printBody() {
        printer.withContext(Context(this)) {
            println(" {")
            withIndent {
                printChildren()
            }
            println("}")
        }
    }

    private fun SirDeclaration.printAttributes() = attributes.render().takeUnless { it.isBlank() }?.let { println(it) }

    private fun SirDeclarationContainer.printChildren() {
        allNonPackageEnums()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allTypealiases()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allProtocols()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allClasses()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        allStructs()
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

    private fun SirVariable.printDeclaration() {
        if (currentContext.declaration !is SirProtocol) {
            printModifiers()
            printOverride()
        }
        print(
            "var ",
            name.swiftIdentifier,
            ": ",
            type.swiftRender,
        )
        println(" {")
        withIndent {
            getter.print()
            setter?.print()
        }
        println("}")
    }

    private fun SirCallable.printDeclaration() {
        if (currentContext.declaration !is SirProtocol) {
            printModifiers()
            printOverride()
        }
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
        printEffects()
        printReturnType()
        if (currentContext.declaration !is SirProtocol) {
            println(" {")
            withIndent {
                body.print()
            }
            println("}")
        } else {
            println()
        }
    }

    private fun SirClassMemberDeclaration.printOverride() {
        if (this.isOverride) {
            print("override ")
        }
    }

    private fun SirCallable.printOverride() {
        when (this) {
            is SirInit -> if (this.isOverride && !this.isRequired) {
                print("override ")
            }
            is SirClassMemberDeclaration -> (this as SirClassMemberDeclaration).printOverride()
            else -> {}
        }
    }

    private fun SirDeclaration.printDocumentation() {
        if (!renderDocComments) return
        documentation?.lines()?.forEach { println(it.trimIndent()) }
    }

    private fun SirImport.print() {
        print(
            when (mode) {
                SirImport.Mode.Exported -> "@_exported "
                SirImport.Mode.ImplementationOnly -> "@_implementationOnly "
                null -> ""
            }
        )
        println("import ${moduleName.swiftIdentifier}")
    }

    private fun SirDeclarationContainer.printContainerKeyword() = print(
        when (this@printContainerKeyword) {
            is SirClass -> "class"
            is SirEnum -> "enum"
            is SirExtension -> "extension"
            is SirStruct -> "struct"
            is SirProtocol -> "protocol"
            is SirModule -> error("there is no keyword for module. Do not print module as declaration container.")
        }
    )

    private val SirDeclaration.inheritedTypes: Pair<SirType?, List<SirProtocol>>
        get() = when (this) {
            is SirClass -> superClass to protocols
            is SirProtocol -> superClass to protocols
            else -> null to emptyList()
        }

    private fun SirDeclaration.printInheritanceClause() {
        val (superclass, interfaces) = this.inheritedTypes

        (listOfNotNull(superclass?.swiftRender) + interfaces.map { it.swiftFqName })
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?.let { print(": $it") }
    }

    private fun SirElement.printName() = print(
        when (this@printName) {
            is SirNamed -> name.swiftIdentifier
            is SirExtension -> extendedType.swiftRender
            else -> error("There is no printable name for SirElement: ${this@printName}")
        }
    )

    private fun SirDeclaration.printVisibility() = print(
        visibility
            .takeUnless { this is SirAccessor }
            .takeIf { it != SirVisibility.INTERNAL }
            ?.let { it.swift + " " }
            ?: ""
    )

    private fun SirClassMemberDeclaration.printModifiers() {
        when (effectiveModality) {
            SirModality.OPEN -> {
                if (visibility == SirVisibility.PUBLIC) {
                    print("open ")
                } else {
                    // Swift properties and methods are internally inheritable
                    // by default – no need to print "open"
                    printVisibility()
                }
                if (callableKind == SirCallableKind.CLASS_METHOD) {
                    print("class ")
                }
            }
            SirModality.FINAL -> {
                printVisibility()
                if (callableKind == SirCallableKind.CLASS_METHOD) {
                    print("static ")
                } else if (callableKind != SirCallableKind.FUNCTION) {
                    // to reduce noise we don't print 'final' when it's implied
                    if ((parent as? SirClass)?.modality != SirModality.FINAL) {
                        print("final ")
                    }
                }
            }
            SirModality.UNSPECIFIED -> {
                printVisibility()
                if (callableKind == SirCallableKind.CLASS_METHOD) {
                    print("class ")
                }
            }
        }
    }

    private fun SirClass.printModifiers() {
        when (modality) {
            SirModality.OPEN -> {
                if (visibility == SirVisibility.PUBLIC) {
                    print("open ")
                } else {
                    // Swift classes are internally inheritable
                    // by default – no need to print "open"
                    printVisibility()
                }
            }
            SirModality.FINAL -> {
                printVisibility()
                print("final ")
            }
            SirModality.UNSPECIFIED -> {
                printVisibility()
            }
        }
    }

    private fun SirDeclaration.printModifiers() {
        if (this is SirClassMemberDeclaration) {
            printModifiers()
        } else if (this is SirClass) {
            printModifiers()
        } else {
            printVisibility()
        }
    }

    private fun SirCallable.printPreNameKeywords() = this.also {
        when (this) {
            is SirInit -> {
                if (isRequired) {
                    print("required ")
                }
                if (isConvenience) {
                    print("convenience ")
                }
            }
            is SirFunction -> {}
            is SirGetter -> print("get")
            is SirSetter -> print("set")
        }
    }

    private fun SirCallable.printName() = print(
        when (this) {
            is SirInit -> "init"
            is SirFunction -> "func ${name.swiftIdentifier}"
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
        is SirFunction -> listOfNotNull(extensionReceiverParameter) + parameters
        is SirInit -> parameters
    }

    private fun SirCallable.printEffects() {
        if (this !is SirSetter && errorType != SirType.never) {
            print(" throws")
            if (errorType != SirType.any) {
                print("(", errorType.swiftRender, ")")
            }
        }
    }

    private fun SirCallable.printReturnType() = print(
        when (this) {
            is SirFunction -> " -> ${returnType.swiftRender}"
            is SirInit,
            is SirGetter,
            is SirSetter,
                -> ""
        }
    )

    private fun List<SirParameter>.print() =
        takeIf { it.isNotEmpty() }
            ?.let {
                println()
                withIndent {
                    this.forEachIndexed { index, sirParameter ->
                        print(sirParameter.swiftRender)
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
        .flatMap { it.split("\n") }
        .forEach {
            println(it)
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


private val SirType.swiftRender: String
    get() = when (this) {
        is SirOptionalType -> wrappedType.swiftRender + "?"
        is SirArrayType -> "[${elementType.swiftRender}]"
        is SirDictionaryType -> "[${keyType.swiftRender}: ${valueType.swiftRender}]"
        else -> swiftName
    }

private val SirClassMemberDeclaration.callableKind: SirCallableKind
    get() = when (this) {
        is SirVariable -> kind
        is SirCallable -> (this as SirCallable).kind
    }

private val SirParameter.swiftRender: String
    get() = (argumentName?.swiftIdentifier ?: "_") +
            (parameterName?.swiftIdentifier?.let { " $it" } ?: "") + ": " +
            (type.attributes.render().takeUnless { it.isBlank() }?.let { "$it " } ?: "") +
            type.swiftRender

private val SirArgument.swiftRender
    get(): String = name?.let { "${it.swiftIdentifier}: ${expression.swiftRender}" } ?: expression.swiftRender

private val SirExpression.swiftRender: String
    get() = when (this) {
        is SirExpression.Raw -> raw
        is SirExpression.StringLiteral -> value.swiftStringLiteral
    }

private fun List<SirAttribute>.render(): String = joinToString(" ") { atr ->
    buildString {
        fun List<SirArgument>.render(): String = joinToString(prefix = "(", postfix = ")") { it.swiftRender }
        append("@")
        append(atr.identifier.swiftIdentifier)
        append(atr.arguments?.render() ?: "")
    }
}
