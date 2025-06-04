/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils;

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual

internal inline val <reified T : SirClassMemberDeclaration> T.overridableCandidates: List<T>
    get() =
        generateSequence((parent as? SirClass)?.superClassDeclaration) { it.superClassDeclaration }
            .flatMap { it.declarations }
            .filterIsInstance<T>()
            .filter { it.modality != SirModality.FINAL && !it.isUnsuitablyDeprecatedToOverride }
            .toList()


internal val SirClassInhertingDeclaration.superClassDeclaration: SirClass? get() = (superClass as? SirNominalType)?.typeDeclaration as? SirClass

internal fun SirType.isSubtypeOf(other: SirType): Boolean = when (this) {
    is SirOptionalType -> (other as? SirOptionalType)?.let { wrappedType.isSubtypeOf(it.wrappedType) } ?: false
    is SirNominalType -> when (other) {
        is SirOptionalType -> this.isSubtypeOf(other.wrappedType)
        is SirNominalType -> if (this.typeDeclaration == other.typeDeclaration) {
            this.typeArguments == other.typeArguments
        } else {
            this.typeDeclaration.isSubclassOf(other.typeDeclaration)
        }
        else -> false
    }
    is SirFunctionalType -> other is SirFunctionalType
            && this.returnType.isSubtypeOf(other.returnType)
            && this.parameterTypes.zipIfSizesAreEqual(other.parameterTypes)?.all { it.second.isSubtypeOf(it.first) } ?: false

    else -> false
}

private fun SirDeclaration.isSubclassOf(other: SirDeclaration): Boolean = this == other || this is SirClass && (superClass as? SirNominalType)?.typeDeclaration?.isSubclassOf(other) ?: false

private fun SirInit.bestOverrideCandidate(): SirInit? = (this.parent as? SirClass)?.superClassDeclaration?.let { cls ->
    cls.overrideableInitializers.firstOrNull { other -> this.parameters.isSuitableForOverrideOf(other.parameters) }
}

internal sealed class OverrideStatus<T: SirDeclaration>(val declaration: T) {
    class Overrides<T: SirDeclaration>(declaration: T): OverrideStatus<T>(declaration)
    class Conflicts<T: SirDeclaration>(declaration: T): OverrideStatus<T>(declaration)
}

private fun<T: SirDeclaration> OverrideStatus(declaration: T, isOverride: Boolean): OverrideStatus<T> = if (isOverride) {
    OverrideStatus.Overrides(declaration)
} else {
    OverrideStatus.Conflicts(declaration)
}

internal fun SirInit.computeIsOverride(): OverrideStatus<SirInit>? = bestOverrideCandidate()?.let {
    OverrideStatus(
        it,
        isOverride = !it.isUnsuitablyDeprecatedToOverride
                && (!this.isFailable || this.isFailable && it.isFailable)
                && (this.errorType.isSubtypeOf(it.errorType))
    )
}

private fun SirFunction.bestOverrideCandidate(): SirFunction? = overridableCandidates.firstOrNull {
    this.name == it.name &&
            this.parameters.isSuitableForOverrideOf(it.parameters) &&
            this.returnType.isSubtypeOf(it.returnType) &&
            this.isInstance == it.isInstance
}

internal fun SirFunction.computeIsOverride(): OverrideStatus<SirFunction>? = bestOverrideCandidate()?.let {
    OverrideStatus(it, !it.isUnsuitablyDeprecatedToOverride && this.errorType.isSubtypeOf(it.errorType))
}

private fun SirVariable.bestOverrideCandidate(): SirVariable? = overridableCandidates.firstOrNull {
    this.name == it.name && this.isInstance == it.isInstance
}

internal fun SirVariable.computeIsOverride(): OverrideStatus<SirVariable>? = bestOverrideCandidate()?.let {
    OverrideStatus(
        it,
        isOverride = !it.isUnsuitablyDeprecatedToOverride
                && (it.setter == null) == (this.setter == null)
                && (this.type != SirType.never || it.type == SirType.never)
                && (it.setter == null && this.type.isSubtypeOf(it.type) || this.type == it.type)
                && (this.getter.errorType.isSubtypeOf(it.getter.errorType))
    )
}

internal fun List<SirParameter>.isSuitableForOverrideOf(other: List<SirParameter>): Boolean =
    this.size == other.size && this.zip(other).all { it.second.type.isSubtypeOf(it.first.type) }

private fun SirInit.isViableOverrideFor(other: SirInit): Boolean =
    this.parameters.isSuitableForOverrideOf(other.parameters) && (!this.isFailable || this.isFailable && other.isFailable)

private val SirClass.overrideableInitializers: List<SirInit>
    // By swift rules:
    // 1) Only inits matching designated initializers from the superclass need to be marked `override`
    // 2) Class inherits its parent's designated initializers if it doesn't itself define any designated (or required) initializers
    get() = this.declarations.filterIsInstance<SirInit>().let { initializers ->
        if (initializers.all { it.isConvenience }) {
            this.superClassDeclaration?.overrideableInitializers ?: emptyList()
        } else {
            initializers.filter { !it.isConvenience }
        }
    }.filter { !it.isUnsuitablyDeprecatedToOverride }

/**
 *  Returns all available initializers to call, which includes both own and initializers inherited from parent
 *  Doesn't take into account initializers generated by the swift compiler and macros
 *
 *  See https://docs.swift.org/swift-book/documentation/the-swift-programming-language/initialization
 */
public fun SirClass.calculateAllAvailableInitializers(): List<SirInit> {
    val parentInitializers = this.superClassDeclaration?.calculateAllAvailableInitializers() ?: emptyList()
    val ownInitializers = this.declarations.filterIsInstance<SirInit>()
    val result = ownInitializers.toMutableList()

    // If the class does not define own designated initializers, it inherits designated initializers from parent
    if (ownInitializers.none { !it.isConvenience }) {
        result.addAll(parentInitializers.filter { !it.isConvenience })
    }

    // If the class overrides or inherits every parent designated initializer, it inherits convenience initializers from its parent
    if (parentInitializers.filter { !it.isConvenience }.all { base -> result.any { it.isViableOverrideFor(base) } }) {
        result.addAll(parentInitializers.filter { it.isConvenience })
    }

    return result
}

private val SirDeclaration.isUnsuitablyDeprecatedToOverride: Boolean
    get() = attributes.findIsInstanceAnd<SirAttribute.Available> { it.unavailable } != null


/**
 * Checks if a declaration directly (within the inheritance clause) or indirectly (through another conformance)
 * declares conformance to [protocol]. This only considers the current declaration (i.e. disregards extensions)
 *
 * @param protocol
 */
internal fun SirDeclaration.declaresConformance(protocol: SirProtocol): Boolean = this == protocol
        || this is SirProtocolConformingDeclaration && protocols.any { it.declaresConformance(protocol) }
        || this is SirClassInhertingDeclaration && (superClassDeclaration?.declaresConformance(protocol) ?: false)