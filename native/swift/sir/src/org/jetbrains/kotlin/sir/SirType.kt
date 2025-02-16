/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftFqName

sealed interface SirType {
    val attributes: List<SirAttribute>

    companion object {
        val any get() = SirExistentialType()
        val never get() = SirNominalType(SirSwiftModule.never)
        val void get() = SirNominalType(SirSwiftModule.void)
    }
}

class SirFunctionalType(
    val parameterTypes: List<SirType>,
    val returnType: SirType,
    override val attributes: List<SirAttribute> = emptyList(),
) : SirType

open class SirNominalType(
    val typeDeclaration: SirNamedDeclaration,
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
}

class SirOptionalType(type: SirType) : SirNominalType(
    typeDeclaration = SirSwiftModule.optional,
    typeArguments = listOf(type)
) {
    val wrappedType: SirType get() = super.typeArguments.single()
}

class SirArrayType(type: SirType): SirNominalType(
    typeDeclaration = SirSwiftModule.array,
    typeArguments = listOf(type)
) {
    val elementType: SirType get() = super.typeArguments.single()
}

class SirDictionaryType(keyType: SirType, valueType: SirType): SirNominalType(
    typeDeclaration = SirSwiftModule.dictionary,
    typeArguments = listOf(keyType, valueType)
) {
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
