/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.util.SirSwiftModule

sealed interface SirType {
    companion object {
        val any get() = SirExistentialType()
        val never get() = SirNominalType(SirSwiftModule.never)
        val void get() = SirNominalType(SirSwiftModule.void)
    }
}

class SirFunctionalType(
    val parameterTypes: List<SirType>,
    val returnType: SirType,
) : SirType

open class SirNominalType(
    val typeDeclaration: SirNamedDeclaration,
    val typeArguments: List<SirType> = emptyList(),
    val parent: SirNominalType? = null,
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

class SirOptionalType(type: SirType): SirNominalType(
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
    // TODO: Protocols. For now, only `any Any` is supported
) : SirType {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other != null && this::class != other::class) return false
        return true
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
class SirErrorType(val reason: String) : SirType

/**
 * A synthetic type for not yet supported Kotlin types.
 */
data object SirUnsupportedType : SirType

fun SirType.optional(): SirNominalType = SirOptionalType(this)
