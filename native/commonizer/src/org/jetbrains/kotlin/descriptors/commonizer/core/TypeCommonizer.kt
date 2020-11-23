/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNodeWithClassId
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

class TypeCommonizer(private val cache: CirClassifiersCache) : AbstractStandardCommonizer<CirType, CirType>() {
    private lateinit var wrapped: Commonizer<*, CirType>

    override fun commonizationResult() = wrapped.result

    override fun initialize(first: CirType) {
        @Suppress("UNCHECKED_CAST")
        wrapped = when (first) {
            is CirClassType -> ClassTypeCommonizer(cache)
            is CirTypeAliasType -> TypeAliasTypeCommonizer(cache)
            is CirTypeParameterType -> TypeParameterTypeCommonizer()
            is CirFlexibleType -> FlexibleTypeCommonizer(cache)
        } as Commonizer<*, CirType>
    }

    override fun doCommonizeWith(next: CirType) = when (next) {
        is CirClassType -> (wrapped as? ClassTypeCommonizer)?.commonizeWith(next) == true
        is CirTypeAliasType -> (wrapped as? TypeAliasTypeCommonizer)?.commonizeWith(next) == true
        is CirTypeParameterType -> (wrapped as? TypeParameterTypeCommonizer)?.commonizeWith(next) == true
        is CirFlexibleType -> (wrapped as? FlexibleTypeCommonizer)?.commonizeWith(next) == true
    }
}

private class ClassTypeCommonizer(private val cache: CirClassifiersCache) : AbstractStandardCommonizer<CirClassType, CirClassType>() {
    private lateinit var classId: ClassId
    private val outerType = OuterClassTypeCommonizer(cache)
    private lateinit var anyVisibility: DescriptorVisibility
    private val arguments = TypeArgumentListCommonizer(cache)
    private var isMarkedNullable = false

    override fun commonizationResult() = CirTypeFactory.createClassType(
        classId = classId,
        outerType = outerType.result,
        // N.B. The 'visibility' field in class types is needed ONLY for TA commonization. The class type constructed here is
        // intended to be used in "common" target. It could not participate in TA commonization. So, it does not matter which
        // exactly visibility will be recorded for commonized class type. Passing the visibility of the first class type
        // to reach better interning rate.
        visibility = anyVisibility,
        arguments = arguments.result,
        isMarkedNullable = isMarkedNullable
    )

    override fun initialize(first: CirClassType) {
        classId = first.classifierId
        anyVisibility = first.visibility
        isMarkedNullable = first.isMarkedNullable
    }

    override fun doCommonizeWith(next: CirClassType) =
        isMarkedNullable == next.isMarkedNullable
                && classId == next.classifierId
                && outerType.commonizeWith(next.outerType)
                && commonizeClassifier(classId) { cache.classNode(classId) }.first
                && arguments.commonizeWith(next.arguments)
}

private class OuterClassTypeCommonizer(cache: CirClassifiersCache) :
    AbstractNullableCommonizer<CirClassType, CirClassType, CirClassType, CirClassType>(
        wrappedCommonizerFactory = { ClassTypeCommonizer(cache) },
        extractor = { it },
        builder = { it }
    )

private class TypeAliasTypeCommonizer(private val cache: CirClassifiersCache) :
    AbstractStandardCommonizer<CirTypeAliasType, CirClassOrTypeAliasType>() {

    private lateinit var typeAliasId: ClassId
    private val arguments = TypeArgumentListCommonizer(cache)
    private var isMarkedNullable = false
    private var commonizedTypeBuilder: CommonizedTypeAliasTypeBuilder? = null // null means not selected yet

    override fun commonizationResult() =
        (commonizedTypeBuilder ?: failInEmptyState()).build(
            typeAliasId = typeAliasId,
            arguments = arguments.result,
            isMarkedNullable = isMarkedNullable
        )

    override fun initialize(first: CirTypeAliasType) {
        typeAliasId = first.classifierId
        isMarkedNullable = first.isMarkedNullable
    }

    override fun doCommonizeWith(next: CirTypeAliasType): Boolean {
        if (isMarkedNullable != next.isMarkedNullable || typeAliasId != next.classifierId)
            return false

        if (commonizedTypeBuilder == null) {
            val (commonized, commonClassifier) = commonizeClassifier(typeAliasId) { cache.typeAliasNode(typeAliasId) }
            if (!commonized)
                return false

            commonizedTypeBuilder = when (commonClassifier) {
                is CirClass -> CommonizedTypeAliasTypeBuilder.forClass(commonClassifier)
                is CirTypeAlias -> CommonizedTypeAliasTypeBuilder.forTypeAlias(commonClassifier)
                null -> CommonizedTypeAliasTypeBuilder.forKnownUnderlyingType(next.underlyingType)
                else -> error("Unexpected common classifier type: ${commonClassifier::class.java}, $commonClassifier")
            }
        }

        return arguments.commonizeWith(next.arguments)
    }

    // builds a new type for "common" library fragment for the given combination of type alias types in "platform" fragments
    private interface CommonizedTypeAliasTypeBuilder {
        fun build(typeAliasId: ClassId, arguments: List<CirTypeProjection>, isMarkedNullable: Boolean): CirClassOrTypeAliasType

        companion object {
            // type alias has been commonized to expect class, need to build type for expect class
            fun forClass(commonClass: CirClass) = object : CommonizedTypeAliasTypeBuilder {
                override fun build(typeAliasId: ClassId, arguments: List<CirTypeProjection>, isMarkedNullable: Boolean) =
                    CirTypeFactory.createClassType(
                        classId = typeAliasId,
                        outerType = null, // there can't be outer type
                        visibility = commonClass.visibility,
                        arguments = arguments,
                        isMarkedNullable = isMarkedNullable
                    )
            }

            // type alias has been commonized to another type alias with the different underlying type, need to build type for
            // new type alias
            fun forTypeAlias(modifiedTypeAlias: CirTypeAlias) = forKnownUnderlyingType(modifiedTypeAlias.underlyingType)

            // type alias don't needs to be commonized because it is from the standard library
            fun forKnownUnderlyingType(underlyingType: CirClassOrTypeAliasType) = object : CommonizedTypeAliasTypeBuilder {
                override fun build(typeAliasId: ClassId, arguments: List<CirTypeProjection>, isMarkedNullable: Boolean): CirTypeAliasType {
                    val underlyingTypeWithProperNullability = if (isMarkedNullable && !underlyingType.isMarkedNullable)
                        CirTypeFactory.makeNullable(underlyingType)
                    else
                        underlyingType

                    return CirTypeFactory.createTypeAliasType(
                        typeAliasId = typeAliasId,
                        underlyingType = underlyingTypeWithProperNullability, // TODO replace arguments???
                        arguments = arguments,
                        isMarkedNullable = isMarkedNullable
                    )
                }
            }
        }
    }
}

private class TypeParameterTypeCommonizer : AbstractStandardCommonizer<CirTypeParameterType, CirTypeParameterType>() {
    private lateinit var temp: CirTypeParameterType

    override fun commonizationResult() = temp

    override fun initialize(first: CirTypeParameterType) {
        temp = first
    }

    override fun doCommonizeWith(next: CirTypeParameterType): Boolean {
        // Real type parameter commonization is performed in TypeParameterCommonizer.
        // Here it is enough to check that type parameter indices and nullability are equal.
        return temp == next
    }
}

private class FlexibleTypeCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirFlexibleType, CirFlexibleType>() {
    private val lowerBound = TypeCommonizer(cache)
    private val upperBound = TypeCommonizer(cache)

    override fun commonizationResult() = CirFlexibleType(
        lowerBound = lowerBound.result as CirSimpleType,
        upperBound = upperBound.result as CirSimpleType
    )

    override fun initialize(first: CirFlexibleType) = Unit

    override fun doCommonizeWith(next: CirFlexibleType) =
        lowerBound.commonizeWith(next.lowerBound) && upperBound.commonizeWith(next.upperBound)
}

private class TypeArgumentCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirTypeProjection, CirTypeProjection>() {
    private var isStar = false
    private lateinit var projectionKind: Variance
    private val type = TypeCommonizer(cache)

    override fun commonizationResult() = if (isStar) CirStarTypeProjection else CirTypeProjectionImpl(
        projectionKind = projectionKind,
        type = type.result
    )

    override fun initialize(first: CirTypeProjection) {
        when (first) {
            is CirStarTypeProjection -> isStar = true
            is CirTypeProjectionImpl -> projectionKind = first.projectionKind
        }
    }

    override fun doCommonizeWith(next: CirTypeProjection) = when (next) {
        is CirStarTypeProjection -> isStar
        is CirTypeProjectionImpl -> !isStar && projectionKind == next.projectionKind && type.commonizeWith(next.type)
    }
}

private class TypeArgumentListCommonizer(cache: CirClassifiersCache) : AbstractListCommonizer<CirTypeProjection, CirTypeProjection>(
    singleElementCommonizerFactory = { TypeArgumentCommonizer(cache) }
)

private inline fun <reified T : CirClassifier> commonizeClassifier(
    classifierId: ClassId,
    classifierNode: (classifierId: ClassId) -> CirNodeWithClassId<*, T>?
): Pair<Boolean, T?> {
    if (classifierId.packageFqName.isUnderStandardKotlinPackages) {
        /* either class or type alias from Kotlin stdlib */
        return true to null
    }

    /* or descriptors themselves can be commonized */
    return when (val node = classifierNode(classifierId)) {
        null -> {
            // No node means that the class or type alias was not subject for commonization at all, probably it lays
            // not in commonized module descriptors but somewhere in their dependencies.
            true to null
        }
        else -> {
            // If entry is present, then contents (common declaration) should not be null.
            val commonClassifier = node.commonDeclaration()
            (commonClassifier != null) to commonClassifier
        }
    }
}
