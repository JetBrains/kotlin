/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
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
        val classifierDescriptor: ClassifierDescriptor = abbreviation.declarationDescriptor

        val simpleType = CirSimpleTypeImpl(
            kind = classifierDescriptor.cirSimpleTypeKind,
            visibility = (classifierDescriptor as? ClassifierDescriptorWithTypeParameters)?.visibility ?: Visibilities.UNKNOWN,
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
