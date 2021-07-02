/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import kotlinx.metadata.KmType
import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode
import org.jetbrains.kotlin.descriptors.Visibility
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

abstract class CirTypeParameterType : CirSimpleType() {
    abstract val index: Int

    override fun appendDescriptionTo(builder: StringBuilder) {
        builder.append('#').append(index)
        super.appendDescriptionTo(builder)
    }

    companion object {
        fun createInterned(
            index: Int,
            isMarkedNullable: Boolean
        ): CirTypeParameterType = interner.intern(
            CirTypeParameterTypeInternedImpl(
                index = index,
                isMarkedNullable = isMarkedNullable
            )
        )

        private val interner = Interner<CirTypeParameterTypeInternedImpl>()
    }
}

sealed class CirClassOrTypeAliasType : CirSimpleType() {
    abstract val classifierId: CirEntityId
    abstract val arguments: List<CirTypeProjection>

    override fun appendDescriptionTo(builder: StringBuilder) = appendDescriptionTo(builder, shortNameOnly = false)

    protected open fun appendDescriptionTo(builder: StringBuilder, shortNameOnly: Boolean) {
        builder.append(if (shortNameOnly) classifierId.relativeNameSegments.last() else classifierId)
        if (arguments.isNotEmpty()) arguments.joinTo(builder, prefix = "<", postfix = ">")
        super.appendDescriptionTo(builder)
    }

    abstract fun withArguments(arguments: List<CirTypeProjection>): CirClassOrTypeAliasType
}

abstract class CirClassType : CirClassOrTypeAliasType(), CirHasVisibility {
    abstract val outerType: CirClassType?

    override fun appendDescriptionTo(builder: StringBuilder, shortNameOnly: Boolean) {
        val outerType = outerType
        if (outerType != null) {
            outerType.appendDescriptionTo(builder)
            builder.append('.')
        }
        super.appendDescriptionTo(builder, shortNameOnly = outerType != null)
    }

    override fun withArguments(arguments: List<CirTypeProjection>): CirClassOrTypeAliasType {
        return createInterned(
            classId = classifierId,
            outerType = outerType,
            visibility = visibility,
            arguments = arguments,
            isMarkedNullable = isMarkedNullable
        )
    }

    companion object {
        fun createInterned(
            classId: CirEntityId,
            outerType: CirClassType?,
            visibility: Visibility,
            arguments: List<CirTypeProjection>,
            isMarkedNullable: Boolean
        ): CirClassType = interner.intern(
            CirClassTypeInternedImpl(
                classifierId = classId,
                outerType = outerType,
                visibility = visibility,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )

        private val interner = Interner<CirClassTypeInternedImpl>()
    }
}

/**
 * All attributes are read from the abbreviation type: [KmType.abbreviatedType].
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

    override fun withArguments(arguments: List<CirTypeProjection>): CirClassOrTypeAliasType {
        return createInterned(
            typeAliasId = classifierId,
            underlyingType = underlyingType,
            arguments = arguments,
            isMarkedNullable = isMarkedNullable
        )
    }

    companion object {
        fun createInterned(
            typeAliasId: CirEntityId,
            underlyingType: CirClassOrTypeAliasType,
            arguments: List<CirTypeProjection>,
            isMarkedNullable: Boolean
        ): CirTypeAliasType = interner.intern(
            CirTypeAliasTypeInternedImpl(
                classifierId = typeAliasId,
                underlyingType = underlyingType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )

        private val interner = Interner<CirTypeAliasTypeInternedImpl>()
    }
}

sealed class CirTypeProjection

object CirStarTypeProjection : CirTypeProjection() {
    override fun toString() = "*"
}

data class CirRegularTypeProjection(val projectionKind: Variance, val type: CirType) : CirTypeProjection() {
    override fun toString() = buildString {
        append(projectionKind)
        if (isNotEmpty()) append(' ')
        type.appendDescriptionTo(this)
    }
}

private data class CirTypeParameterTypeInternedImpl(
    override val index: Int,
    override val isMarkedNullable: Boolean
) : CirTypeParameterType()

private class CirClassTypeInternedImpl(
    override val classifierId: CirEntityId,
    override val outerType: CirClassType?,
    override val visibility: Visibility, // visibility of the class descriptor
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean,
) : CirClassType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(classifierId)
        .appendHashCode(outerType)
        .appendHashCode(visibility)
        .appendHashCode(arguments)
        .appendHashCode(isMarkedNullable)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirClassType -> classifierId == other.classifierId
                && isMarkedNullable == other.isMarkedNullable
                && visibility == other.visibility
                && arguments == other.arguments
                && outerType == other.outerType
        else -> false
    }
}

private class CirTypeAliasTypeInternedImpl(
    override val classifierId: CirEntityId,
    override val underlyingType: CirClassOrTypeAliasType,
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean
) : CirTypeAliasType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(classifierId)
        .appendHashCode(underlyingType)
        .appendHashCode(arguments)
        .appendHashCode(isMarkedNullable)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirTypeAliasType -> classifierId == other.classifierId
                && underlyingType == other.underlyingType
                && isMarkedNullable == other.isMarkedNullable
                && arguments == other.arguments
        else -> false
    }
}
