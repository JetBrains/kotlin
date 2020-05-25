/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleTypeKind.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirSimpleTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.declarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqNameInterned
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqNameWithTypeParameters
import org.jetbrains.kotlin.types.*

object CirTypeFactory {
    private val interner = Interner<CirSimpleType>()

    fun create(source: KotlinType): CirType = source.unwrap().run {
        when (this) {
            is SimpleType -> create(this)
            is FlexibleType -> CirFlexibleType(create(lowerBound), create(upperBound))
        }
    }

    fun create(source: SimpleType): CirSimpleType {
        val abbreviation: SimpleType = (source as? AbbreviatedType)?.abbreviation ?: source
        val expanded: SimpleType = (source as? AbbreviatedType)?.expandedType ?: source

        val simpleType = CirSimpleTypeImpl(
            kind = abbreviation.declarationDescriptor.cirSimpleTypeKind,
            fqName = abbreviation.fqNameInterned,
            arguments = abbreviation.arguments.map { projection ->
                CirTypeProjection(
                    projectionKind = projection.projectionKind,
                    isStarProjection = projection.isStarProjection,
                    type = create(projection.type)
                )
            },
            isMarkedNullable = abbreviation.isMarkedNullable,
            isDefinitelyNotNullType = abbreviation.isDefinitelyNotNullType,
            expandedTypeConstructorId = CirTypeConstructorId(
                fqName = expanded.fqNameInterned,
                numberOfTypeParameters = expanded.constructor.parameters.size
            ),
            fqNameWithTypeParameters = source.fqNameWithTypeParameters
        )

        return interner.intern(simpleType)
    }
}

val ClassifierDescriptor.cirSimpleTypeKind: CirSimpleTypeKind
    get() = when (this) {
        is ClassDescriptor -> CLASS
        is TypeAliasDescriptor -> TYPE_ALIAS
        is TypeParameterDescriptor -> TYPE_PARAMETER
        else -> error("Unexpected classifier descriptor type: ${this::class.java}, $this")
    }
