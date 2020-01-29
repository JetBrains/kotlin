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
import org.jetbrains.kotlin.descriptors.commonizer.utils.declarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqName
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqNameWithTypeParameters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.*
import kotlin.LazyThreadSafetyMode.PUBLICATION

sealed class CirType {
    companion object {
        fun create(type: KotlinType): CirType = type.unwrap().run {
            when (this) {
                is SimpleType -> CirSimpleType(this)
                is FlexibleType -> CirFlexibleType(CirSimpleType(lowerBound), CirSimpleType(upperBound))
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
 * There is no difference between [abbreviation] and [expanded] for types representing classes and type parameters.
 */
class CirSimpleType(private val wrapped: SimpleType) : CirType() {
    val annotations by lazy(PUBLICATION) { abbreviation.annotations.map(::CirAnnotation) }
    val kind = CirSimpleTypeKind.determineKind(abbreviation.declarationDescriptor)
    val fqName by lazy(PUBLICATION) { abbreviation.fqName }
    val arguments by lazy(PUBLICATION) { abbreviation.arguments.map(::CirTypeProjection) }
    val isMarkedNullable get() = abbreviation.isMarkedNullable
    val isDefinitelyNotNullType get() = abbreviation.isDefinitelyNotNullType
    val expandedTypeConstructorId by lazy(PUBLICATION) { CirTypeConstructorId(expanded) }

    inline val isClassOrTypeAlias get() = (kind == CLASS || kind == TYPE_ALIAS)
    val fqNameWithTypeParameters by lazy(PUBLICATION) { wrapped.fqNameWithTypeParameters }

    override fun equals(other: Any?) = wrapped == (other as? CirSimpleType)?.wrapped
    override fun hashCode() = wrapped.hashCode()

    private inline val abbreviation: SimpleType get() = (wrapped as? AbbreviatedType)?.abbreviation ?: wrapped
    private inline val expanded: SimpleType get() = (wrapped as? AbbreviatedType)?.expandedType ?: wrapped
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
    constructor(type: SimpleType) : this(type.fqName, type.constructor.parameters.size)
}

class CirTypeProjection(private val wrapped: TypeProjection) {
    val projectionKind get() = wrapped.projectionKind
    val isStarProjection get() = wrapped.isStarProjection
    val type by lazy(PUBLICATION) { CirType.create(wrapped.type) }
}

data class CirFlexibleType(val lowerBound: CirSimpleType, val upperBound: CirSimpleType) : CirType()

val CirType.fqNameWithTypeParameters: String
    get() = when (this) {
        is CirSimpleType -> fqNameWithTypeParameters
        is CirFlexibleType -> lowerBound.fqNameWithTypeParameters
    }
