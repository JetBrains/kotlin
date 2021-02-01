/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirTypeAliasTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.*

object CirTypeFactory {
    object StandardTypes {
        val ANY: CirClassType = createClassType(
            classId = ANY_CLASS_ID,
            outerType = null,
            visibility = DescriptorVisibilities.PUBLIC,
            arguments = emptyList(),
            isMarkedNullable = false
        )
    }

    private val classTypeInterner = Interner<CirClassType>()
    private val typeAliasTypeInterner = Interner<CirTypeAliasType>()
    private val typeParameterTypeInterner = Interner<CirTypeParameterType>()

    fun create(source: KotlinType, useAbbreviation: Boolean = true): CirType = source.unwrap().run {
        when (this) {
            is SimpleType -> create(this, useAbbreviation)
            is FlexibleType -> CirFlexibleType(create(lowerBound, useAbbreviation), create(upperBound, useAbbreviation))
        }
    }

    fun create(source: SimpleType, useAbbreviation: Boolean): CirSimpleType {
        if (useAbbreviation && source is AbbreviatedType) {
            val abbreviation = source.abbreviation
            when (val classifierDescriptor = abbreviation.declarationDescriptor) {
                is TypeAliasDescriptor -> {
                    return createTypeAliasType(
                        typeAliasId = classifierDescriptor.internedClassId,
                        underlyingType = create(extractExpandedType(source), useAbbreviation = true) as CirClassOrTypeAliasType,
                        arguments = createArguments(abbreviation.arguments, useAbbreviation = true),
                        isMarkedNullable = abbreviation.isMarkedNullable
                    )
                }
                else -> error("Unexpected classifier descriptor type for abbreviation type: ${classifierDescriptor::class.java}, $classifierDescriptor, ${source.abbreviation}")
            }
        }

        return when (val classifierDescriptor = source.declarationDescriptor) {
            is ClassDescriptor -> createClassTypeWithAllOuterTypes(
                classDescriptor = classifierDescriptor,
                arguments = createArguments(source.arguments, useAbbreviation),
                isMarkedNullable = source.isMarkedNullable
            )
            is TypeAliasDescriptor -> {
                val abbreviatedType = TypeAliasExpander.NON_REPORTING.expand(
                    TypeAliasExpansion.create(null, classifierDescriptor, source.arguments),
                    Annotations.EMPTY
                ) as AbbreviatedType

                val expandedType = extractExpandedType(abbreviatedType)

                val cirExpandedType = create(expandedType, useAbbreviation = true) as CirClassOrTypeAliasType
                val cirExpandedTypeWithProperNullability = if (source.isMarkedNullable) makeNullable(cirExpandedType) else cirExpandedType

                createTypeAliasType(
                    typeAliasId = classifierDescriptor.internedClassId,
                    underlyingType = cirExpandedTypeWithProperNullability,
                    arguments = createArguments(source.arguments, useAbbreviation = true),
                    isMarkedNullable = source.isMarkedNullable
                )
            }
            is TypeParameterDescriptor -> createTypeParameterType(classifierDescriptor.typeParameterIndex, source.isMarkedNullable)
            else -> error("Unexpected classifier descriptor type: ${classifierDescriptor::class.java}, $classifierDescriptor, $source")
        }
    }

    fun createClassType(
        classId: ClassId,
        outerType: CirClassType?,
        visibility: DescriptorVisibility,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirClassType {
        return classTypeInterner.intern(
            CirClassTypeImpl(
                classifierId = classId,
                outerType = outerType,
                visibility = visibility,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    fun createTypeAliasType(
        typeAliasId: ClassId,
        underlyingType: CirClassOrTypeAliasType,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirTypeAliasType {
        return typeAliasTypeInterner.intern(
            CirTypeAliasTypeImpl(
                classifierId = typeAliasId,
                underlyingType = underlyingType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createTypeParameterType(
        index: Int,
        isMarkedNullable: Boolean
    ): CirTypeParameterType {
        return typeParameterTypeInterner.intern(
            CirTypeParameterType(
                index = index,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    fun makeNullable(classOrTypeAliasType: CirClassOrTypeAliasType): CirClassOrTypeAliasType =
        if (classOrTypeAliasType.isMarkedNullable)
            classOrTypeAliasType
        else
            when (classOrTypeAliasType) {
                is CirClassType -> createClassType(
                    classId = classOrTypeAliasType.classifierId,
                    outerType = classOrTypeAliasType.outerType,
                    visibility = classOrTypeAliasType.visibility,
                    arguments = classOrTypeAliasType.arguments,
                    isMarkedNullable = true
                )
                is CirTypeAliasType -> createTypeAliasType(
                    typeAliasId = classOrTypeAliasType.classifierId,
                    underlyingType = makeNullable(classOrTypeAliasType.underlyingType),
                    arguments = classOrTypeAliasType.arguments,
                    isMarkedNullable = true
                )
            }

    private fun createClassTypeWithAllOuterTypes(
        classDescriptor: ClassDescriptor,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirClassType {
        val outerType: CirClassType?
        val remainingArguments: List<CirTypeProjection>

        if (classDescriptor.isInner) {
            val declaredTypeParametersCount = classDescriptor.declaredTypeParameters.size
            outerType = createClassTypeWithAllOuterTypes(
                classDescriptor = classDescriptor.containingDeclaration as ClassDescriptor,
                arguments = arguments.subList(declaredTypeParametersCount, arguments.size),
                isMarkedNullable = false // don't pass nullable flag to outer types
            )
            remainingArguments = arguments.subList(0, declaredTypeParametersCount)
        } else {
            outerType = null
            remainingArguments = arguments
        }

        return createClassType(
            classId = classDescriptor.internedClassId,
            outerType = outerType,
            visibility = classDescriptor.visibility,
            arguments = remainingArguments,
            isMarkedNullable = isMarkedNullable
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun createArguments(arguments: List<TypeProjection>, useAbbreviation: Boolean): List<CirTypeProjection> =
        arguments.compactMap { projection ->
            if (projection.isStarProjection)
                CirStarTypeProjection
            else
                CirTypeProjectionImpl(
                    projectionKind = projection.projectionKind,
                    type = create(projection.type, useAbbreviation)
                )
        }

    private inline val TypeParameterDescriptor.typeParameterIndex: Int
        get() {
            var index = index
            var parent = containingDeclaration

            if (parent is CallableMemberDescriptor) {
                parent = parent.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return index
                index += parent.declaredTypeParameters.size
            }

            while (parent is ClassifierDescriptorWithTypeParameters) {
                parent = parent.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: break
                index += parent.declaredTypeParameters.size
            }

            return index
        }
}
