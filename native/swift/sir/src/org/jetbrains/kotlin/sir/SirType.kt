/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirType

class SirNominalType(
    val type: SirNamedDeclaration,
    val parent: SirNominalType? = null,
) : SirType {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other != null && this::class != other::class) return false

        other as SirNominalType

        if (type != other.type) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
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
