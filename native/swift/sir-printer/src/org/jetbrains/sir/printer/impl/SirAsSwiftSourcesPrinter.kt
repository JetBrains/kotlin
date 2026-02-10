/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.kotlin.sir.util.returnType
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.withIndent
import kotlin.sequences.filter

private data class Context(val declaration: SirDeclarationParent)

internal class SirAsSwiftSourcesPrinter private constructor(
    private val printer: ContextualizedPrinter<Context>,
    private val stableDeclarationsOrder: Boolean,
    private val renderDocComments: Boolean,
    private val renderDeclarationOrigins: Boolean,
    private val emptyBodyStub: SirFunctionBody
) : ContextualizedPrinter<Context> by printer {
    public companion object {

        public fun print(
            module: SirModule,
            stableDeclarationsOrder: Boolean,
            renderDocComments: Boolean,
            renderDeclarationOrigins: Boolean,
            emptyBodyStub: SirFunctionBody
        ): String {
            val childrenPrinter = SirAsSwiftSourcesPrinter(
                ContextualizedPrinterImpl(SmartPrinter(StringBuilder()), Context(module)),
                stableDeclarationsOrder = stableDeclarationsOrder,
                renderDocComments = renderDocComments,
                renderDeclarationOrigins = renderDeclarationOrigins,
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
                    renderDeclarationOrigins = renderDeclarationOrigins,
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

        if (renderDeclarationOrigins) {
            println("// $origin")
        }

        printAttributes()

        when (this) {
            is SirClass -> printDeclaration()
            is SirEnum -> printDeclaration()
            is SirEnumCase -> "case $name"
            is SirExtension -> printDeclaration()
            is SirStruct -> printDeclaration()
            is SirProtocol -> printDeclaration()
            is SirCallable -> printDeclaration()
            is SirVariable -> printDeclaration()
            is SirTypealias -> printDeclaration()
            is SirSubscript -> printDeclaration()
        }
    }

    private fun SirTypealias.printDeclaration() {
        printVisibility()
        print("typealias ")
        printName()
        print(" = ")
        println(type.swiftRender(SirTypeVariance.INVARIANT))
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
        if (this.protocols.isEmpty()) {
            printVisibility()
        }
        print("extension ")
        printName()
        printInheritanceClause()
        printWhereClause()
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
                if (this is SirEnum) {
                    for (case in cases) {
                        println("case ${case.name}")
                    }
                }
                printChildren()
            }
            println("}")
        }
    }

    private fun SirDeclaration.printAttributes() = attributes
        .render(SirTypeVariance.INVARIANT)
        .takeUnless { it.isBlank() }
        ?.let { println(it) }

    private fun SirDeclarationContainer.printChildren() = with(this.declarations.toList()) {
        filterIsInstanceAnd<SirEnum> { it.origin !is SirOrigin.Namespace }
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        filterIsInstance<SirTypealias>()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        filterIsInstance<SirProtocol>()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        filterIsInstance<SirClass>()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        filterIsInstance<SirStruct>()
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
        filterIsInstance<SirVariable>()
            .sortedWithIfNeeded(Comparators.stableVariableComparator)
            .forEach { it.print() }
        filterIsInstance<SirCallable>()
            .sortedWithIfNeeded(Comparators.stableCallableComparator)
            .forEach { it.print() }
        filterIsInstance<SirSubscript>()
            .sortedWithIfNeeded(Comparators.stableSubscriptComparator)
            .forEach { it.print() }
        if (this@printChildren is SirModule) {
            this@with.filterIsInstance<SirExtension>()
                .sortedWithIfNeeded(Comparators.stableExtensionComparator)
                .forEach { it.print() }
        }
        filterIsInstanceAnd<SirEnum> { it.origin is SirOrigin.Namespace }
            .sortedWithIfNeeded(Comparators.stableNamedComparator)
            .forEach { it.print() }
    }

    private inline fun <reified T : SirElement> List<T>.sortedWithIfNeeded(comparator: Comparator<in T>): List<T> =
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
            type.swiftRender(SirTypeVariance.INVARIANT),
        )
        println(" {")
        withIndent {
            getter.print()
            setter?.print()
        }
        println("}")
    }

    private fun SirSubscript.printDeclaration() {
        if (currentContext.declaration !is SirProtocol) {
            printModifiers()
            printOverride()
        }
        print("subscript(")
        parameters.print()
        print(")")
        print(" -> ${returnType.swiftRender(SirTypeVariance.CONTRAVARIANT)}")

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
        if (this is SirAccessor) {
            if (this is SirSetter && this.parameterName != "newValue") {
                // newValue is the default implicit setter parameter name in swift
                print("(", this.parameterName, ")")
            }
        } else {
            print("(")
            collectParameters().print()
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
            is SirExtension -> null to protocols
            is SirEnum -> null to protocols
            else -> null to emptyList()
        }

    private fun SirDeclaration.printInheritanceClause() {
        val (superclass, interfaces) = this.inheritedTypes

        (listOfNotNull(superclass?.swiftRender(SirTypeVariance.INVARIANT)) + interfaces.map { it.swiftFqName })
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?.let { print(": $it") }
    }

    private fun SirConstrainedDeclaration.printWhereClause() {
        constraints.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "where ") {
            listOf(
                (it.subjectPath.takeIf { it.isNotEmpty() }?.joinToString(separator = ".") ?: "Self"),
                when (it) {
                    is SirTypeConstraint.Conformance -> ":"
                    is SirTypeConstraint.Equality -> "=="
                },
                it.constraint.swiftRenderAsConstraint
            ).joinToString(separator = " ")
        }?.let { print(" $it") }
    }

    private fun SirElement.printName() = print(
        when (this@printName) {
            is SirScopeDefiningElement -> name.swiftIdentifier
            is SirExtension -> extendedType.swiftRender(SirTypeVariance.INVARIANT)
            else -> error("There is no printable name for SirElement: ${this@printName}")
        }
    )

    private fun SirDeclaration.printVisibility() = print(
        visibility
            .takeUnless { this is SirAccessor }
            .takeUnless { this is SirExtension && this.visibility == SirVisibility.PUBLIC }
            .takeUnless { currentContext.declaration is SirProtocol }
            ?.let { (currentContext.declaration as? SirExtension)?.let { decl -> minOf(decl.visibility, it) } ?: it }
            .takeIf { it != SirVisibility.INTERNAL }
            ?.let { it.swift + " " }
            ?: ""
    )

    private fun SirClassMemberDeclaration.printModifiers() {
        when (this.parent) {
            is SirModule -> {
                printVisibility()
            }

            is SirExtension, is SirProtocol, is SirStruct, is SirEnum -> {
                printVisibility()
                if (callableKind == SirCallableKind.CLASS_METHOD) {
                    print("static ")
                }
            }

            is SirSubscript, is SirVariable -> {
                // nothing
            }

            is SirClass -> when (effectiveModality) {
                SirModality.OPEN -> {
                    if (visibility == SirVisibility.PUBLIC) {
                        print("open ")
                    } else {
                        // Swift properties and methods are internally inheritable
                        // by default – no need to print "open"
                        printVisibility()
                    }
                    if (callableKind == SirCallableKind.CLASS_METHOD) {
                        print(if (this.parent is SirClass) "class " else "static ")
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
                        print(if (this.parent is SirClass) "class " else "static ")
                    }
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
            is SirFunction -> {
                when (fixity) {
                    SirFixity.PREFIX -> print("prefix ")
                    SirFixity.POSTFIX -> print("postfix ")
                    null, SirFixity.INFIX -> { /* Swift doesn't allow explicitly stating infix even though it is an existing keyword. */ }
                }
            }
            is SirGetter -> print("get")
            is SirSetter -> print("set")
        }
    }

    private fun SirCallable.printName() = print(
        when (this) {
            is SirInit -> "init"
            is SirFunction -> "func ${name.takeIf { this.fixity != null && it.isValidSwiftOperator } ?: name.swiftIdentifier}"
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
        is SirFunction -> listOfNotNull(contextParameter(), extensionReceiverParameter) + parameters
        is SirInit -> parameters
    }

    private fun SirFunction.contextParameter(): SirParameter? {
        val parameters = contextParameters
        if (parameters.isEmpty()) return null
        val withNames = parameters.size > 1
        val types = parameters.map { it.parameterName.takeIf { withNames } to it.type }
        return SirParameter(
            parameterName = "context",
            type = SirTupleType(types),
        )
    }

    private fun SirCallable.printEffects() {
        if (this !is SirSetter && isAsync) {
            print(" async")
        }

        if (this !is SirSetter && errorType != SirType.never) {
            print(" throws")
            if (errorType != SirType.any) {
                print("(", errorType.swiftRender(SirTypeVariance.COVARIANT), ")")
            }
        }
    }

    private fun SirCallable.printReturnType() = print(
        when (this) {
            is SirFunction -> " -> ${returnType.swiftRender(SirTypeVariance.COVARIANT)}"
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


    private fun SirType.swiftRender(position: SirTypeVariance): String {
        val attributesString = (attributes.render(position).takeUnless { it.isBlank() }?.let { "$it " } ?: "")
        val renderedType = "Self"
            .takeIf {
                currentContext.declaration
                    .let { it is SirExtension && it.extendedType == this && it.extendedType.isBivariantSelf == true }
            }
            ?: when (this) {
                is SirImplicitlyUnwrappedOptionalType if wrappedType !is SirWrappedType -> {
                    wrappedType.swiftRender(position).let { if (it.any { it.isWhitespace() }) "($it)" else it } + "!"
                }
                is SirOptionalType -> wrappedType.swiftRender(SirTypeVariance.INVARIANT).let { if (it.any { it.isWhitespace() }) "($it)" else it } + "?"
                is SirArrayType ->
                    "[${elementType.swiftRender(SirTypeVariance.INVARIANT)}]"
                is SirDictionaryType ->
                    "[${keyType.swiftRender(SirTypeVariance.INVARIANT)}: ${valueType.swiftRender(SirTypeVariance.INVARIANT)}]"

                is SirFunctionalType ->
                    "(${parameterTypes.render()})${" async throws".takeIf { isAsync } ?: ""} -> ${returnType.swiftRender(SirTypeVariance.COVARIANT)}"

                is SirTupleType ->
                    "(${types.joinToString { (name, type) -> "${name?.let { "$it: " } ?: ""}${type.swiftRender(SirTypeVariance.INVARIANT)}" }})"

                else -> swiftName
            }
        return attributesString + renderedType
    }

    private fun List<SirType>.render() = joinToString { it.swiftRender(SirTypeVariance.CONTRAVARIANT) }

    private val SirType.swiftRenderAsConstraint: String
        get() = when (this) {
            is SirExistentialType -> protocols.takeIf { it.isNotEmpty() }?.joinToString(separator = " & ") { it.swiftFqName } ?: "Any"
            else -> this.swiftRender(SirTypeVariance.INVARIANT)
        }

    private val SirParameter.swiftRender: String
        get() = (argumentName?.swiftIdentifier ?: "_") +
                (parameterName?.swiftIdentifier?.let { " $it" } ?: "") + ": " +
                type.swiftRender((SirTypeVariance.CONTRAVARIANT)) +
                if (isVariadic) "..." else ""
}

private val SirVisibility.swift
    get(): String = when (this) {
        SirVisibility.PRIVATE -> "private"
        SirVisibility.FILEPRIVATE -> "fileprivate"
        SirVisibility.INTERNAL -> "internal"
        SirVisibility.PUBLIC -> "public"
        SirVisibility.PACKAGE -> "package"
    }

private val SirClassMemberDeclaration.callableKind: SirCallableKind
    get() = when (this) {
        is SirVariable -> kind
        is SirCallable -> (this as SirCallable).kind
        is SirSubscript -> kind
    }

private val SirArgument.swiftRender
    get(): String = name?.let { "${it.swiftIdentifier}: ${expression.swiftRender}" } ?: expression.swiftRender

private val SirExpression.swiftRender: String
    get() = when (this) {
        is SirExpression.Raw -> raw
        is SirExpression.StringLiteral -> value.swiftStringLiteral
    }

private fun List<SirAttribute>.render(position: SirTypeVariance): String = mapNotNull { atr ->
    buildString {
        fun List<SirArgument>.render(): String = joinToString(prefix = "(", postfix = ")") { it.swiftRender }
        append("@")
        append(atr.identifier.swiftIdentifier)
        append(atr.arguments?.render() ?: "")
    }
        .takeIf { (atr as? SirFunctionalTypeAttribute)?.isPrintableInPosition(position) ?: true }
}.joinToString(" ")


private val SirType.isBivariantSelf: Boolean? get() = when (this) {
        is SirErrorType, is SirUnsupportedType -> null
        is SirExistentialType, is SirFunctionalType -> true
        is SirTupleType -> false
        is SirNominalType -> parent == null && typeArguments.isEmpty() && typeDeclaration !is SirClass /* also not actors */
    }
