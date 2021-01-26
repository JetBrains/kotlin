/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizedTypeAliasAnswer.Companion.FAILURE_MISSING_IN_SOME_TARGET
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizedTypeAliasAnswer.Companion.SUCCESS_FROM_DEPENDEE_LIBRARY
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.name.ClassId

class TypeCommonizer(private val classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirType, CirType>() {
    private lateinit var wrapped: Commonizer<*, CirType>

    override fun commonizationResult() = wrapped.result

    override fun initialize(first: CirType) {
        @Suppress("UNCHECKED_CAST")
        wrapped = when (first) {
            is CirClassType -> ClassTypeCommonizer(classifiers)
            is CirTypeAliasType -> TypeAliasTypeCommonizer(classifiers)
            is CirTypeParameterType -> TypeParameterTypeCommonizer()
            is CirFlexibleType -> FlexibleTypeCommonizer(classifiers)
        } as Commonizer<*, CirType>
    }

    override fun doCommonizeWith(next: CirType) = when (next) {
        is CirClassType -> (wrapped as? ClassTypeCommonizer)?.commonizeWith(next) == true
        is CirTypeAliasType -> (wrapped as? TypeAliasTypeCommonizer)?.commonizeWith(next) == true
        is CirTypeParameterType -> (wrapped as? TypeParameterTypeCommonizer)?.commonizeWith(next) == true
        is CirFlexibleType -> (wrapped as? FlexibleTypeCommonizer)?.commonizeWith(next) == true
    }
}

private class ClassTypeCommonizer(private val classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirClassType, CirClassType>() {
    private lateinit var classId: ClassId
    private val outerType = OuterClassTypeCommonizer(classifiers)
    private lateinit var anyVisibility: DescriptorVisibility
    private val arguments = TypeArgumentListCommonizer(classifiers)
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
                && commonizeClass(classId, classifiers)
                && arguments.commonizeWith(next.arguments)
}

private class OuterClassTypeCommonizer(classifiers: CirKnownClassifiers) :
    AbstractNullableCommonizer<CirClassType, CirClassType, CirClassType, CirClassType>(
        wrappedCommonizerFactory = { ClassTypeCommonizer(classifiers) },
        extractor = { it },
        builder = { it }
    )

private class TypeAliasTypeCommonizer(private val classifiers: CirKnownClassifiers) :
    AbstractStandardCommonizer<CirTypeAliasType, CirClassOrTypeAliasType>() {

    private lateinit var typeAliasId: ClassId
    private val arguments = TypeArgumentListCommonizer(classifiers)
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
            val answer = commonizeTypeAlias(typeAliasId, classifiers)
            if (!answer.commonized)
                return false

            commonizedTypeBuilder = when (val commonClassifier = answer.commonClassifier) {
                is CirClass -> CommonizedTypeAliasTypeBuilder.forClass(commonClassifier)
                is CirTypeAlias -> CommonizedTypeAliasTypeBuilder.forTypeAlias(commonClassifier)
                null -> {
                    val underlyingType = computeSuitableUnderlyingType(classifiers, next.underlyingType) ?: return false
                    CommonizedTypeAliasTypeBuilder.forKnownUnderlyingType(underlyingType)
                }
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

private class FlexibleTypeCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirFlexibleType, CirFlexibleType>() {
    private val lowerBound = TypeCommonizer(classifiers)
    private val upperBound = TypeCommonizer(classifiers)

    override fun commonizationResult() = CirFlexibleType(
        lowerBound = lowerBound.result as CirSimpleType,
        upperBound = upperBound.result as CirSimpleType
    )

    override fun initialize(first: CirFlexibleType) = Unit

    override fun doCommonizeWith(next: CirFlexibleType) =
        lowerBound.commonizeWith(next.lowerBound) && upperBound.commonizeWith(next.upperBound)
}

private fun commonizeClass(classId: ClassId, classifiers: CirKnownClassifiers): Boolean {
    if (classifiers.commonDependeeLibraries.hasClassifier(classId)) {
        // The class is from common fragment of dependee library (ex: stdlib). Already commonized.
        return true
    } else if (classId.packageFqName.isUnderKotlinNativeSyntheticPackages) {
        // C/Obj-C forward declarations are:
        // - Either resolved to real classes/interfaces from other interop libraries (which are generated by C-interop tool and
        //   are known to have modality/visibility/other attributes to successfully pass commonization).
        // - Or resolved to the same synthetic classes/interfaces.
        // ... and therefore are considered as successfully commonized.
        return true
    }

    return when (val node = classifiers.commonized.classNode(classId)) {
        null -> {
            // No node means that the class was not subject for commonization.
            // - Either it is missing in certain targets at all => not commonized.
            // - Or it is a known forward declaration => consider it as commonized.
            classifiers.forwardDeclarations.isExportedForwardDeclaration(classId)
        }
        else -> {
            // Common declaration in node is not null -> successfully commonized.
            (node.commonDeclaration() != null)
        }
    }
}

private fun commonizeTypeAlias(typeAliasId: ClassId, classifiers: CirKnownClassifiers): CommonizedTypeAliasAnswer {
    if (classifiers.commonDependeeLibraries.hasClassifier(typeAliasId)) {
        // The type alias is from common fragment of dependee library (ex: stdlib). Already commonized.
        return SUCCESS_FROM_DEPENDEE_LIBRARY
    }

    return when (val node = classifiers.commonized.typeAliasNode(typeAliasId)) {
        null -> {
            // No node means that the type alias was not subject for commonization. It is missing in some target(s) => not commonized.
            FAILURE_MISSING_IN_SOME_TARGET
        }
        else -> {
            // Common declaration in node is not null -> successfully commonized.
            CommonizedTypeAliasAnswer.create(node.commonDeclaration())
        }
    }
}

private class CommonizedTypeAliasAnswer(val commonized: Boolean, val commonClassifier: CirClassifier?) {
    companion object {
        val SUCCESS_FROM_DEPENDEE_LIBRARY = CommonizedTypeAliasAnswer(true, null)
        val FAILURE_MISSING_IN_SOME_TARGET = CommonizedTypeAliasAnswer(false, null)

        fun create(commonClassifier: CirClassifier?) =
            if (commonClassifier != null) CommonizedTypeAliasAnswer(true, commonClassifier) else FAILURE_MISSING_IN_SOME_TARGET
    }
}
