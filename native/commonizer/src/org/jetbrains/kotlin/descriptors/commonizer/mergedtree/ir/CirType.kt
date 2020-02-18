/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirSimpleTypeKind.CLASS
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirSimpleTypeKind.TYPE_ALIAS
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.*

sealed class CirType {
    companion object {
        fun create(type: KotlinType): CirType = type.unwrap().run {
            when (this) {
                is SimpleType -> CirSimpleType.create(this)
                is FlexibleType -> CirFlexibleType(CirSimpleType.create(lowerBound), CirSimpleType.create(upperBound))
            }
        }
    }
}

/**
 * All attributes except for [expandedTypeConstructorId] are read from the abbreviation type: [AbbreviatedType.abbreviation].
 * And [expandedTypeConstructorId] is read from the expanded type: [AbbreviatedType.expandedType].
 *
 * This is necessary to properly compare types for type aliases, where abbreviation type represents the type alias itself while
 * expanded type represents right-hand side declaration that should be processed separately.
 *
 * There is no difference between "abbreviation" and "expanded" for types representing classes and type parameters.
 */
class CirSimpleType private constructor(original: SimpleType) : CirType() {
    val annotations: List<CirAnnotation>
    val kind: CirSimpleTypeKind
    val fqName: FqName
    val arguments: List<CirTypeProjection>
    val isMarkedNullable: Boolean
    val isDefinitelyNotNullType: Boolean
    val expandedTypeConstructorId: CirTypeConstructorId

    init {
        val abbreviation = (original as? AbbreviatedType)?.abbreviation ?: original
        val expanded = (original as? AbbreviatedType)?.expandedType ?: original

        annotations = abbreviation.annotations.map(CirAnnotation.Companion::create)
        kind = CirSimpleTypeKind.determineKind(abbreviation.declarationDescriptor)
        fqName = abbreviation.fqNameInterned
        arguments = abbreviation.arguments.map(::CirTypeProjection)
        isMarkedNullable = abbreviation.isMarkedNullable
        isDefinitelyNotNullType = abbreviation.isDefinitelyNotNullType
        expandedTypeConstructorId = CirTypeConstructorId(expanded)
    }

    inline val isClassOrTypeAlias get() = (kind == CLASS || kind == TYPE_ALIAS)
    val fqNameWithTypeParameters = original.fqNameWithTypeParameters

    // Note: equals() and hashCode() are implemented in a way that only right-hand side declaration
    // is compared for typealiases (well, actually "fqNameWithTypeParameters" for the right-hand side).
    // This is sufficient for unit tests and for evaluating commonized supertypes, but is absolutely
    // unsuitable for comparison of CirSimpleType for the purposes of interning, etc.
    override fun equals(other: Any?) = fqNameWithTypeParameters == (other as? CirSimpleType)?.fqNameWithTypeParameters
    override fun hashCode() = fqNameWithTypeParameters.hashCode()

    private class Interned(val type: CirSimpleType) {
        // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
        private var cachedHashCode = 0

        private fun computeHashCode(): Int {
            var result = type.annotations.hashCode()
            result = 31 * result + type.kind.hashCode()
            result = 31 * result + type.fqName.hashCode()
            result = 31 * result + type.arguments.hashCode()
            result = 31 * result + type.isMarkedNullable.hashCode()
            result = 31 * result + type.isDefinitelyNotNullType.hashCode()
            result = 31 * result + type.fqNameWithTypeParameters.hashCode()
            return result
        }

        override fun hashCode(): Int {
            var currentHashCode = cachedHashCode
            if (currentHashCode != 0) return currentHashCode

            currentHashCode = computeHashCode()
            cachedHashCode = currentHashCode
            return currentHashCode
        }

        override fun equals(other: Any?): Boolean =
            if (other is Interned) {
                type.isMarkedNullable == other.type.isMarkedNullable
                        && type.fqName == other.type.fqName
                        && type.kind == other.type.kind
                        && type.arguments == other.type.arguments
                        && type.fqNameWithTypeParameters == other.type.fqNameWithTypeParameters
                        && type.annotations == other.type.annotations
                        && type.isDefinitelyNotNullType == other.type.isDefinitelyNotNullType
            } else
                false
    }

    companion object {
        private val interner = NonThreadSafeInterner<Interned>()

        fun create(original: SimpleType): CirSimpleType = interner.intern(Interned(CirSimpleType(original))).type
    }
}

enum class CirSimpleTypeKind {
    CLASS,
    TYPE_ALIAS,
    TYPE_PARAMETER;

    companion object {
        fun determineKind(classifier: ClassifierDescriptor) = when (classifier) {
            is ClassDescriptor -> CLASS
            is TypeAliasDescriptor -> TYPE_ALIAS
            is TypeParameterDescriptor -> TYPE_PARAMETER
            else -> error("Unexpected classifier descriptor type: ${classifier::class.java}, $classifier")
        }

        fun areCompatible(expect: CirSimpleTypeKind, actual: CirSimpleTypeKind) =
            expect == actual || (expect == CLASS && actual == TYPE_ALIAS)
    }
}

data class CirTypeConstructorId(val fqName: FqName, val numberOfTypeParameters: Int) {
    constructor(type: SimpleType) : this(type.fqNameInterned, type.constructor.parameters.size)
}

data class CirTypeProjection(
    val projectionKind: Variance,
    val isStarProjection: Boolean,
    val type: CirType
) {
    constructor(original: TypeProjection) : this(original.projectionKind, original.isStarProjection, CirType.create(original.type))
}

data class CirFlexibleType(val lowerBound: CirSimpleType, val upperBound: CirSimpleType) : CirType()

val CirType.fqNameWithTypeParameters: String
    get() = when (this) {
        is CirSimpleType -> fqNameWithTypeParameters
        is CirFlexibleType -> lowerBound.fqNameWithTypeParameters
    }
