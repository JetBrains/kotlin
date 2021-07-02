/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.core.CommonizedTypeAliasAnswer.Companion.FAILURE_MISSING_IN_SOME_TARGET
import org.jetbrains.kotlin.commonizer.core.CommonizedTypeAliasAnswer.Companion.SUCCESS_FROM_DEPENDENCY_LIBRARY
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

internal class TypeAliasTypeCommonizer(private val classifiers: CirKnownClassifiers) :
    AbstractStandardCommonizer<CirTypeAliasType, CirClassOrTypeAliasType>() {

    private lateinit var typeAliasId: CirEntityId
    private val arguments = TypeArgumentListCommonizer(classifiers)
    private val underlyingTypeArguments = TypeArgumentListCommonizer(classifiers)
    private var isMarkedNullable = false
    private var commonizedTypeBuilder: CommonizedTypeAliasTypeBuilder? = null // null means not selected yet

    override fun commonizationResult() =
        (commonizedTypeBuilder ?: failInEmptyState()).build(
            typeAliasId = typeAliasId,
            arguments = arguments.result,
            underlyingTypeArguments = underlyingTypeArguments.result,
            isMarkedNullable = isMarkedNullable
        )

    override fun initialize(first: CirTypeAliasType) {
        typeAliasId = first.classifierId
        isMarkedNullable = first.expandedType().isMarkedNullable
    }

    override fun doCommonizeWith(next: CirTypeAliasType): Boolean {
        if (isMarkedNullable != next.expandedType().isMarkedNullable || typeAliasId != next.classifierId)
            return false

        if (commonizedTypeBuilder == null) {
            val answer = commonizeTypeAlias(typeAliasId, classifiers)
            if (!answer.isCommon)
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

        return arguments.commonizeWith(next.arguments) &&
                underlyingTypeArguments.commonizeWith(next.underlyingType.arguments)
    }

    // builds a new type for "common" library fragment for the given combination of type alias types in "platform" fragments
    internal interface CommonizedTypeAliasTypeBuilder {
        fun build(
            typeAliasId: CirEntityId,
            arguments: List<CirTypeProjection>,
            underlyingTypeArguments: List<CirTypeProjection>,
            isMarkedNullable: Boolean
        ): CirClassOrTypeAliasType

        companion object {
            // type alias has been commonized to expect class, need to build type for expect class
            fun forClass(commonClass: CirClass) = object : CommonizedTypeAliasTypeBuilder {
                override fun build(
                    typeAliasId: CirEntityId,
                    arguments: List<CirTypeProjection>,
                    underlyingTypeArguments: List<CirTypeProjection>,
                    isMarkedNullable: Boolean
                ) = CirClassType.createInterned(
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
                override fun build(
                    typeAliasId: CirEntityId,
                    arguments: List<CirTypeProjection>,
                    underlyingTypeArguments: List<CirTypeProjection>,
                    isMarkedNullable: Boolean
                ): CirTypeAliasType {
                    val underlyingTypeWithProperNullability = underlyingType
                        .makeNullableIfNecessary(isMarkedNullable)
                        .withArguments(underlyingTypeArguments)

                    return CirTypeAliasType.createInterned(
                        typeAliasId = typeAliasId,
                        underlyingType = underlyingTypeWithProperNullability,
                        arguments = arguments,
                        isMarkedNullable = isMarkedNullable
                    )
                }
            }
        }
    }
}

private fun commonizeTypeAlias(typeAliasId: CirEntityId, classifiers: CirKnownClassifiers): CommonizedTypeAliasAnswer {
    if (classifiers.commonDependencies.hasClassifier(typeAliasId)) {
        // The type alias is from common fragment of dependency library (ex: stdlib). Already commonized.
        return SUCCESS_FROM_DEPENDENCY_LIBRARY
    }

    val typeAliasNode = classifiers.commonizedNodes.typeAliasNode(typeAliasId)
    val classNode = classifiers.commonizedNodes.classNode(typeAliasId)

    if (typeAliasNode == null && classNode == null) {
        return FAILURE_MISSING_IN_SOME_TARGET
    }

    return CommonizedTypeAliasAnswer.create(typeAliasNode?.commonDeclaration?.invoke() ?: classNode?.commonDeclaration?.invoke())
}

private class CommonizedTypeAliasAnswer(val isCommon: Boolean, val commonClassifier: CirClassifier?) {
    companion object {
        val SUCCESS_FROM_DEPENDENCY_LIBRARY = CommonizedTypeAliasAnswer(true, null)
        val FAILURE_MISSING_IN_SOME_TARGET = CommonizedTypeAliasAnswer(false, null)

        fun create(commonClassifier: CirClassifier?) =
            if (commonClassifier != null) CommonizedTypeAliasAnswer(true, commonClassifier) else FAILURE_MISSING_IN_SOME_TARGET
    }
}
