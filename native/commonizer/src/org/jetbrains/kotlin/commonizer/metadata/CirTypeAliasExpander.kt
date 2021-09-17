/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.metadata

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.cir.CirProvided
import org.jetbrains.kotlin.commonizer.utils.compactMapIndexed
import org.jetbrains.kotlin.types.Variance

class CirTypeAliasExpansion(
    val typeAlias: CirProvided.TypeAlias,
    val arguments: List<CirTypeProjection>,
    val isMarkedNullable: Boolean,
    val typeResolver: CirTypeResolver
) {
    companion object {
        fun create(
            typeAliasId: CirEntityId,
            arguments: List<CirTypeProjection>,
            isMarkedNullable: Boolean,
            typeResolver: CirTypeResolver
        ): CirTypeAliasExpansion {
            val typeAlias: CirProvided.TypeAlias = typeResolver.resolveClassifier(typeAliasId)
            checkArgumentsCount(typeAlias, typeAliasId, arguments)

            return CirTypeAliasExpansion(
                typeAlias = typeAlias,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable,
                typeResolver = typeResolver
            )
        }
    }
}

object CirTypeAliasExpander {
    fun expand(expansion: CirTypeAliasExpansion): CirClassOrTypeAliasType {
        val underlyingType = expansion.typeAlias.underlyingType
        val underlyingProjection = CirProvided.RegularTypeProjection(Variance.INVARIANT, underlyingType)

        val expandedProjection = expandTypeProjection(expansion, underlyingProjection, Variance.INVARIANT)
        check(expandedProjection is CirRegularTypeProjection) {
            "Type alias expansion: result for $underlyingType is $expandedProjection, should not be a star projection"
        }
        check(expandedProjection.projectionKind == Variance.INVARIANT) {
            "Type alias expansion: result for $underlyingType is $expandedProjection, should be invariant"
        }

        val expandedType = expandedProjection.type as CirClassOrTypeAliasType
        return expandedType.makeNullableIfNecessary(expansion.isMarkedNullable)
    }

    private fun expandTypeProjection(
        expansion: CirTypeAliasExpansion,
        projection: CirProvided.TypeProjection,
        typeParameterVariance: Variance
    ): CirTypeProjection {
        val type = when (projection) {
            is CirProvided.StarTypeProjection -> return CirStarTypeProjection
            is CirProvided.RegularTypeProjection -> projection.type
        }

        val argument = when (type) {
            is CirProvided.TypeParameterType -> expansion.arguments[type.index]
            is CirProvided.TypeAliasType -> {
                val substitutedType = expandTypeAliasType(expansion, type)
                CirRegularTypeProjection(projection.variance, substitutedType)
            }
            is CirProvided.ClassType -> {
                val substitutedType = expandClassType(expansion, type)
                CirRegularTypeProjection(projection.variance, substitutedType)
            }
        }

        return when (argument) {
            is CirStarTypeProjection -> CirStarTypeProjection
            is CirRegularTypeProjection -> {
                val argumentType = argument.type as CirSimpleType

                val resultingVariance = run {
                    val argumentVariance = argument.projectionKind
                    val underlyingVariance = projection.variance

                    val substitutionVariance = when {
                        underlyingVariance == argumentVariance -> argumentVariance
                        underlyingVariance == Variance.INVARIANT -> argumentVariance
                        argumentVariance == Variance.INVARIANT -> underlyingVariance
                        else -> argumentVariance
                    }

                    when {
                        typeParameterVariance == substitutionVariance -> substitutionVariance
                        typeParameterVariance == Variance.INVARIANT -> substitutionVariance
                        substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
                        else -> substitutionVariance
                    }
                }

                val substitutedType = argumentType.makeNullableIfNecessary(type.isMarkedNullable)

                CirRegularTypeProjection(resultingVariance, substitutedType)
            }
        }
    }

    private fun expandTypeAliasType(
        expansion: CirTypeAliasExpansion,
        type: CirProvided.TypeAliasType
    ): CirTypeAliasType {
        val typeAlias: CirProvided.TypeAlias = expansion.typeResolver.resolveClassifier(type.classifierId)
        checkArgumentsCount(typeAlias, type.classifierId, type.arguments)

        val expandedArguments = type.arguments.compactMapIndexed { index, argument ->
            val projection = expandTypeProjection(expansion, argument, typeAlias.typeParameters[index].variance)
            makeNullableTypeIfNecessary(projection, argument)
        }

        val nestedExpansion = CirTypeAliasExpansion(typeAlias, expandedArguments, type.isMarkedNullable, expansion.typeResolver)
        val nestedExpandedType = expand(nestedExpansion)

        return CirTypeAliasType.createInterned(
            typeAliasId = type.classifierId,
            underlyingType = nestedExpandedType,
            arguments = expandedArguments,
            isMarkedNullable = type.isMarkedNullable
        )
    }

    private fun expandClassType(
        expansion: CirTypeAliasExpansion,
        type: CirProvided.ClassType
    ): CirClassType {
        val clazz: CirProvided.Class = expansion.typeResolver.resolveClassifier(type.classifierId)
        checkArgumentsCount(clazz, type.classifierId, type.arguments)

        val expandedArguments = type.arguments.compactMapIndexed { index, argument ->
            val projection = expandTypeProjection(expansion, argument, clazz.typeParameters[index].variance)
            makeNullableTypeIfNecessary(projection, argument)
        }

        return CirClassType.createInterned(
            classId = type.classifierId,
            outerType = type.outerType?.let { expandClassType(expansion, it) },
            arguments = expandedArguments,
            isMarkedNullable = type.isMarkedNullable
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun makeNullableTypeIfNecessary(
        projection: CirTypeProjection,
        originalArgument: CirProvided.TypeProjection
    ): CirTypeProjection {
        return when (projection) {
            is CirStarTypeProjection -> CirStarTypeProjection
            is CirRegularTypeProjection -> {
                val originalTypeIsNullable = (originalArgument as? CirProvided.RegularTypeProjection)?.type?.isMarkedNullable == true
                if (!originalTypeIsNullable)
                    return projection

                val projectionType = projection.type as CirSimpleType
                if (projectionType.isMarkedNullable)
                    return projection

                CirRegularTypeProjection(
                    projectionKind = projection.projectionKind,
                    type = projectionType.makeNullable()
                )
            }
        }
    }
}

private fun checkArgumentsCount(classifier: CirProvided.Classifier, classifierId: CirEntityId, arguments: List<*>) =
    check(classifier.typeParameters.size == arguments.size) {
        "${classifier::class.java.simpleName} $classifierId has different number of type parameters than the number of supplied arguments: ${classifier.typeParameters.size} != ${arguments.size}"
    }
