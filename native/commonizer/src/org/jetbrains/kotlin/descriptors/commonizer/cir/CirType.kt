/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.Variance

typealias CirTypeSignature = String

/**
 * The hierarchy of [CirType]:
 *
 *                 [CirType]
 *                 /       \
 * [CirFlexibleType]       [CirSimpleType]
 *                          /           \
 *     [CirTypeParameterType]           [CirClassOrTypeAliasType]
 *                                            /           \
 *                               [CirClassType]           [CirTypeAliasType]
 */
sealed class CirType {
    final override fun toString() = buildString { appendDescriptionTo(this) }
    internal abstract fun appendDescriptionTo(builder: StringBuilder)
}

data class CirFlexibleType(val lowerBound: CirSimpleType, val upperBound: CirSimpleType) : CirType() {
    override fun appendDescriptionTo(builder: StringBuilder) {
        builder.append("lower = ")
        lowerBound.appendDescriptionTo(builder)
        builder.append(", upper = ")
        upperBound.appendDescriptionTo(builder)
    }
}

/**
 * Note: Annotations at simple types are not preserved. After commonization all annotations assigned to types will be lost.
 */
sealed class CirSimpleType : CirType() {
    abstract val isMarkedNullable: Boolean

    override fun appendDescriptionTo(builder: StringBuilder) {
        if (isMarkedNullable) builder.append('?')
    }
}

data class CirTypeParameterType(
    val index: Int,
    override val isMarkedNullable: Boolean
) : CirSimpleType() {
    override fun appendDescriptionTo(builder: StringBuilder) {
        builder.append('#').append(index)
        super.appendDescriptionTo(builder)
    }
}

sealed class CirClassOrTypeAliasType : CirSimpleType() {
    abstract val classifierId: ClassId
    abstract val arguments: List<CirTypeProjection>

    override fun appendDescriptionTo(builder: StringBuilder) {
        builder.append(classifierId.asString())
        if (arguments.isNotEmpty()) arguments.joinTo(builder, prefix = "<", postfix = ">")
        super.appendDescriptionTo(builder)
    }
}

abstract class CirClassType : CirClassOrTypeAliasType(), CirHasVisibility {
    abstract val outerType: CirClassType?

    override fun appendDescriptionTo(builder: StringBuilder) {
        outerType?.let {
            it.appendDescriptionTo(builder)
            builder.append('.')
        }
        super.appendDescriptionTo(builder)
    }
}

/**
 * All attributes are read from the abbreviation type: [AbbreviatedType.abbreviation].
 *
 * This is necessary to properly compare types for type aliases, where abbreviation type represents the type alias itself while
 * expanded type represents right-hand side declaration that should be processed separately.
 */
abstract class CirTypeAliasType : CirClassOrTypeAliasType() {
    abstract val underlyingType: CirClassOrTypeAliasType

    override fun appendDescriptionTo(builder: StringBuilder) {
        super.appendDescriptionTo(builder)
        builder.append(" -> ")
        underlyingType.appendDescriptionTo(builder)
    }
}

sealed class CirTypeProjection

object CirStarTypeProjection : CirTypeProjection() {
    override fun toString() = "*"
}

data class CirTypeProjectionImpl(val projectionKind: Variance, val type: CirType) : CirTypeProjection() {
    override fun toString() = buildString {
        append(projectionKind)
        if (isNotEmpty()) append(' ')
        type.appendDescriptionTo(this)
    }
}
