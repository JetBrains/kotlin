/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirType

sealed interface SirNominalType : SirType {

    val parent: SirNominalType?

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

class SirClassType(
    val declaration: SirClass,
    override val parent: SirNominalType? = null,
) : SirNominalType {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SirClassType

        if (declaration != other.declaration) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaration.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
}

class SirStructType(
    val declaration: SirStruct,
    override val parent: SirNominalType? = null,
) : SirNominalType {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SirStructType

        if (declaration != other.declaration) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaration.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
}

class SirEnumType(
    val declaration: SirEnum,
    override val parent: SirNominalType? = null,
) : SirNominalType {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SirEnumType

        if (declaration != other.declaration) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaration.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
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
 * it might be an incomplete declaration in IDE or declaration from an not imported library.
 *
 */
object SirUnknownType : SirType

/**
 * A synthetic type for not yet supported Kotlin types.
 */
object SirUnsupportedType : SirType
