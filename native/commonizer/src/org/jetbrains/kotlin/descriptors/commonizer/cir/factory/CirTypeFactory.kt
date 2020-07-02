/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirSimpleTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.declarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.signature
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

        return create(
            classifierId = CirClassifierIdFactory.create(classifierDescriptor),
            visibility = (classifierDescriptor as? ClassifierDescriptorWithTypeParameters)?.visibility ?: Visibilities.UNKNOWN,
            arguments = abbreviation.arguments.map { projection ->
                CirTypeProjection(
                    projectionKind = projection.projectionKind,
                    isStarProjection = projection.isStarProjection,
                    type = create(projection.type)
                )
            },
            isMarkedNullable = abbreviation.isMarkedNullable,
            isDefinitelyNotNullType = abbreviation.isDefinitelyNotNullType,
            signature = source.signature
        )
    }

    fun create(
        classifierId: CirClassifierId,
        visibility: Visibility,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean,
        isDefinitelyNotNullType: Boolean,
        signature: CirTypeSignature
    ): CirSimpleType {
        return interner.intern(
            CirSimpleTypeImpl(
                classifierId = classifierId,
                visibility = visibility,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable,
                isDefinitelyNotNullType = isDefinitelyNotNullType,
                signature = signature
            )
        )
    }
}
