/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.expandedType
import org.jetbrains.kotlin.sir.util.swiftFqName
import kotlin.collections.plus

sealed interface SirType {
    val attributes: List<SirAttribute>

    companion object {
        val any get() = SirExistentialType()
        val anyHashable get() = SirNominalType(SirSwiftModule.anyHashable)
        val never get() = SirNominalType(SirSwiftModule.never)
        val void get() = SirNominalType(SirSwiftModule.void)
    }
}

/**
 * This marker interface underscores that the type contains some unwrapped type(s) inside.
 * It's important mainly for rendering purposes.
 */
sealed interface SirWrappedType : SirType

class SirFunctionalType(
    val parameterTypes: List<SirType>,
    val isAsync: Boolean = false,
    val returnType: SirType,
    override val attributes: List<SirAttribute> = emptyList(),
) : SirWrappedType {
    fun copyAppendingAttributes(vararg attributes: SirAttribute): SirFunctionalType {
        val attributesToAdd = attributes.filter { !this.attributes.contains(it) }
        return if (attributesToAdd.isEmpty()) this
        else SirFunctionalType(parameterTypes, isAsync, returnType, this.attributes + attributesToAdd)
    }
}

class SirTupleType(
    val types: List<Pair<String?, SirType>>,
    override val attributes: List<SirAttribute> = emptyList(),
) : SirWrappedType {
    init {
        val supportsNames = types.size > 1 // 0 = Void, 1 = technically not a tuple
        require(supportsNames || types.all { it.first == null }) {
            "Named tuple types are not supported for tuples with ${types.size} types"
        }
    }
}

open class SirNominalType(
    val typeDeclaration: SirScopeDefiningDeclaration,
    val typeArguments: List<SirType> = emptyList(),
    val parent: SirNominalType? = null,
    override val attributes: List<SirAttribute> = emptyList(),
) : SirType {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SirNominalType) return false

        // Please consider special handling here if a SirNominalType's subtype with incompatible `equals` would appear.
        // This may be required to preserve equals commutativity.

        if (typeDeclaration != other.typeDeclaration) return false
        if (typeArguments != other.typeArguments) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = typeDeclaration.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }

    fun copyAppendingAttributes(vararg attributes: SirAttribute): SirNominalType =
        SirNominalType(typeDeclaration, typeArguments, parent, this.attributes + attributes)
}

open class SirOptionalType(type: SirType) : SirNominalType(
    typeDeclaration = SirSwiftModule.optional,
    typeArguments = listOf(type)
), SirWrappedType {
    val wrappedType: SirType get() = super.typeArguments.single()
}

class SirImplicitlyUnwrappedOptionalType(type: SirType) : SirOptionalType(type)

class SirArrayType(type: SirType) : SirNominalType(
    typeDeclaration = SirSwiftModule.array,
    typeArguments = listOf(type),
), SirWrappedType {
    val elementType: SirType get() = super.typeArguments.single()
}

class SirDictionaryType(keyType: SirType, valueType: SirType) : SirNominalType(
    typeDeclaration = SirSwiftModule.dictionary,
    typeArguments = listOf(keyType, valueType)
), SirWrappedType {
    val keyType: SirType get() = super.typeArguments[0]
    val valueType: SirType get() = super.typeArguments[1]
}

class SirExistentialType(
    protocols: List<SirProtocol>,
) : SirType {
    override val attributes: List<SirAttribute> = emptyList()

    val protocols: List<SirProtocol> = protocols.sortedBy { it.swiftFqName }

    constructor(vararg protocols: SirProtocol) : this(protocols.toList())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SirExistentialType) return false
        return protocols == other.protocols
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

val SirNominalType.escaping: SirNominalType get() = copyAppendingAttributes(SirAttribute.Escaping)

val SirFunctionalType.escaping: SirFunctionalType get() = copyAppendingAttributes(SirAttribute.Escaping)

val SirType.escaping: SirType get() = when (this) {
        is SirNominalType if (this.typeDeclaration as? SirTypealias)?.expandedType?.let { it is SirFunctionalType } == true ->
            escaping
        is SirFunctionalType -> escaping
        else ->
            this
    }

/**
 * A synthetic type for unknown Kotlin types. For example,
 * it might be an incomplete declaration in IDE or declaration from a not imported library.
 *
 */
class SirErrorType(val reason: String) : SirType {
    override val attributes: List<SirAttribute> = emptyList()
}

/**
 * A synthetic type for not yet supported Kotlin types.
 */
data object SirUnsupportedType : SirType {
    override val attributes: List<SirAttribute> = emptyList()
}

fun SirType.optional(): SirNominalType = SirOptionalType(this)

fun SirType.implicitlyUnwrappedOptional(): SirNominalType = SirImplicitlyUnwrappedOptionalType(this)

fun SirScopeDefiningDeclaration.nominalType(parameterTypes: List<SirType> = emptyList()): SirNominalType =
    SirNominalType(
        this,
        parameterTypes,
        attributes = if (this is SirTypealias && this.type is SirFunctionalType) this.type.attributes else emptyList()
    )
