/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.OptimisticNumberCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.PlatformIntegerCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.commonizer.utils.safeCastValues
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ClassOrTypeAliasTypeCommonizer(
    private val typeCommonizer: TypeCommonizer,
    private val classifiers: CirKnownClassifiers,
    private val isOptimisticNumberTypeCommonizationEnabled: Boolean,
    private val isPlatformIntegerCommonizationEnabled: Boolean,
) : NullableSingleInvocationCommonizer<CirClassOrTypeAliasType> {

    constructor(typeCommonizer: TypeCommonizer, classifiers: CirKnownClassifiers, settings: CommonizerSettings) : this(
        typeCommonizer, classifiers,
        settings.getSetting(OptimisticNumberCommonizationEnabledKey),
        settings.getSetting(PlatformIntegerCommonizationEnabledKey),
    )

    private val isMarkedNullableCommonizer = TypeNullabilityCommonizer(typeCommonizer.context)
    private val platformIntegerCommonizer = PlatformIntegerCommonizer(typeCommonizer, classifiers)
    private val typeDistanceMeasurement = TypeDistanceMeasurement(typeCommonizer.context)

    override fun invoke(values: List<CirClassOrTypeAliasType>): CirClassOrTypeAliasType? {
        if (values.isEmpty()) return null
        val expansions = values.map { it.expandedType() }
        val isMarkedNullable = isMarkedNullableCommonizer.commonize(expansions.map { it.isMarkedNullable }) ?: return null

        @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE")
        val substitutedTypes = substituteTypesIfNecessary(values)
            ?: isPlatformIntegerCommonizationEnabled.ifTrue {
                platformIntegerCommonizer(expansions)?.makeNullableIfNecessary(isMarkedNullable)?.let { return it }
            } ?: isOptimisticNumberTypeCommonizationEnabled.ifTrue {
                OptimisticNumbersTypeCommonizer.commonize(expansions)?.makeNullableIfNecessary(isMarkedNullable)?.let { return it }
            } ?: return null

        val classifierId = substitutedTypes.singleDistinctValueOrNull { it.classifierId } ?: return null

        val arguments = TypeArgumentListCommonizer(typeCommonizer).commonize(substitutedTypes.map { it.arguments }) ?: return null

        val outerTypes = substitutedTypes.safeCastValues<CirClassOrTypeAliasType, CirClassType>()?.map { it.outerType }
        val outerType = when {
            outerTypes == null -> null
            outerTypes.all { it == null } -> null
            outerTypes.any { it == null } -> return null
            else -> invoke(outerTypes.map { checkNotNull(it) }) as? CirClassType ?: return null
        }

        /*
        Classifiers under this package (forward declarations) can always be used in common
         */
        if (classifierId.packageName.isUnderKotlinNativeSyntheticPackages) {
            return CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        }

        /*
        Classifier is coming from common dependencies and therefore the type can be used in common
         */
        when (val dependencyClassifier = classifiers.commonDependencies.classifier(classifierId)) {
            is CirProvided.Class -> return CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )

            is CirProvided.TypeAlias -> return CirTypeAliasType.createInterned(
                typeAliasId = classifierId,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable,
                underlyingType = dependencyClassifier.underlyingType.toCirClassOrTypeAliasTypeOrNull(classifiers.commonDependencies)
                    ?.withParentArguments(arguments, isMarkedNullable) ?: return null
            )

            else -> Unit
        }

        /*
        Classifier is coming from 'sources' and is commonized and therefore the type can be used in common
         */
        val commonizedClassifier = classifiers.commonizedNodes.classNode(classifierId)?.commonDeclaration?.invoke()
            ?: classifiers.commonizedNodes.typeAliasNode(classifierId)?.commonDeclaration?.invoke()

        return when (commonizedClassifier) {
            is CirClass -> CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )

            is CirTypeAlias -> CirTypeAliasType.createInterned(
                typeAliasId = classifierId,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable,
                underlyingType = commonizedClassifier.underlyingType.withParentArguments(arguments, isMarkedNullable)
            )

            else -> null
        }
    }

    private fun substituteTypesIfNecessary(types: List<CirClassOrTypeAliasType>): List<CirClassOrTypeAliasType>? {
        /* No substitution is necessary if all types use the same classifierId */
        if (types.singleDistinctValueOrNull { it.classifierId } != null) return types
        val classifierId = selectSubstitutionClassifierId(types) ?: return null
        return types.mapIndexed { targetIndex, type -> substituteIfNecessary(targetIndex, type, classifierId) ?: return null }
    }

    private fun substituteIfNecessary(
        targetIndex: Int, sourceType: CirClassOrTypeAliasType, destinationClassifierId: CirEntityId
    ): CirClassOrTypeAliasType? {
        if (sourceType.classifierId == destinationClassifierId) {
            return sourceType
        }

        if (sourceType is CirTypeAliasType) {
            forwardSubstitute(sourceType, destinationClassifierId)?.let { return it }
        }

        val resolvedClassifierFromDependencies = classifiers.commonDependencies.classifier(destinationClassifierId)
            ?: classifiers.targetDependencies[targetIndex].classifier(destinationClassifierId) // necessary?

        if (resolvedClassifierFromDependencies != null && resolvedClassifierFromDependencies is CirProvided.TypeAlias) {
            return backwardsSubstitute(targetIndex, sourceType, destinationClassifierId, resolvedClassifierFromDependencies)
        }

        val resolvedClassifier = classifiers.classifierIndices[targetIndex].findClassifier(destinationClassifierId)
        if (resolvedClassifier != null && resolvedClassifier is CirTypeAlias) {
            return backwardsSubstitute(sourceType, destinationClassifierId, resolvedClassifier)
        }

        return null
    }

    private fun forwardSubstitute(
        sourceType: CirTypeAliasType,
        destinationClassifierId: CirEntityId,
    ): CirClassOrTypeAliasType? {
        return generateSequence(sourceType.underlyingType) { type -> type.safeAs<CirTypeAliasType>()?.underlyingType }
            .firstOrNull { underlyingType -> underlyingType.classifierId == destinationClassifierId }
    }

    private fun backwardsSubstitute(
        sourceType: CirClassOrTypeAliasType,
        destinationTypeAliasId: CirEntityId,
        destinationTypeAlias: CirTypeAlias
    ): CirTypeAliasType? {
        /*
        Limitation: We do not support 'backwards' type substitution if either source or destination types are parameterized.
        Selecting reasonable arguments for such types would be complicated and we do not know real life APIs that would indeed
        benefit from support.
         */
        if (sourceType.arguments.isNotEmpty()) return null
        if (destinationTypeAlias.typeParameters.isNotEmpty()) return null

        return CirTypeAliasType.createInterned(
            destinationTypeAliasId,
            underlyingType = destinationTypeAlias.underlyingType,
            arguments = emptyList(),
            isMarkedNullable = sourceType.isMarkedNullable
        )
    }

    private fun backwardsSubstitute(
        targetIndex: Int,
        sourceType: CirClassOrTypeAliasType,
        destinationTypeAliasId: CirEntityId,
        destinationTypeAlias: CirProvided.TypeAlias
    ): CirClassOrTypeAliasType? {
        /*
        Limitation: We do not support 'backwards type substitution with arguments.
        See 'backwardsSubstitute implementation for CirTypeAlias for more details.
         */
        if (sourceType.arguments.isNotEmpty()) return null
        if (destinationTypeAlias.typeParameters.isNotEmpty()) return null
        val providedClassifiers = CirProvidedClassifiers.of(classifiers.commonDependencies, classifiers.targetDependencies[targetIndex])

        return CirTypeAliasType.createInterned(
            destinationTypeAliasId,
            underlyingType = destinationTypeAlias.underlyingType.toCirClassOrTypeAliasTypeOrNull(providedClassifiers) ?: return null,
            arguments = emptyList(),
            isMarkedNullable = sourceType.isMarkedNullable
        )
    }

    /**
     * Will select *the* associated classifier that is
     * - reachable from all [types] on all platforms
     * - Has the lowest penalty score (where penalty score will be the maximum penalty on all platforms)
     *
     * Will return null if
     * - No substitution is allowed
     * - The input [types] do not have a single distinct set of associated ids
     */
    private fun selectSubstitutionClassifierId(types: List<CirClassOrTypeAliasType>): CirEntityId? {
        val forwardSubstitutionAllowed = typeCommonizer.context.enableForwardTypeAliasSubstitution
        val backwardsSubstitutionAllowed = typeCommonizer.context.enableBackwardsTypeAliasSubstitution

        /* No substitution allowed in any direction */
        if (!forwardSubstitutionAllowed && !backwardsSubstitutionAllowed) {
            return null
        }

        val associatedIds = types.singleDistinctValueOrNull {
            classifiers.associatedIdsResolver.resolveAssociatedIds(it.classifierId)
        } ?: return null

        val typeSubstitutionCandidates = resolveTypeSubstitutionCandidates(associatedIds, types)
            .onEach { typeSubstitutionCandidate ->
                assert(typeSubstitutionCandidate.typeDistance.isZero.not()) { "Expected no zero typeDistance" }
                assert(typeSubstitutionCandidate.typeDistance.isReachable) { "Expected substitution candidate to be reachable" }
            }

        return typeSubstitutionCandidates.minByOrNull { it.typeDistance.penalty }?.id
    }

    private fun resolveTypeSubstitutionCandidates(
        associatedIds: AssociatedClassifierIds, types: List<CirClassOrTypeAliasType>
    ): List<TypeSubstitutionCandidate> {
        return associatedIds.ids.mapNotNull mapCandidateId@{ candidateId ->
            val typeDistances = types.mapIndexed { targetIndex, type ->
                typeDistanceMeasurement(classifiers, targetIndex, type, candidateId)
                    .takeIf { it.isReachable } ?: return@mapCandidateId null
            }
            TypeSubstitutionCandidate(
                id = candidateId, typeDistance = checkNotNull(typeDistances.maxByOrNull { it.penalty })
            )
        }
    }
}

private class TypeSubstitutionCandidate(
    val id: CirEntityId,
    val typeDistance: CirTypeDistance
)

private interface TypeDistanceMeasurement {
    operator fun invoke(
        classifiers: CirKnownClassifiers, targetIndex: Int, from: CirClassOrTypeAliasType, to: CirEntityId
    ): CirTypeDistance

    private object None : TypeDistanceMeasurement {
        override fun invoke(
            classifiers: CirKnownClassifiers, targetIndex: Int, from: CirClassOrTypeAliasType, to: CirEntityId
        ): CirTypeDistance = CirTypeDistance.unreachable
    }

    private object ForwardOnly : TypeDistanceMeasurement {
        override fun invoke(
            classifiers: CirKnownClassifiers, targetIndex: Int, from: CirClassOrTypeAliasType, to: CirEntityId
        ): CirTypeDistance = forwardTypeDistance(from, to)
    }

    private object Full : TypeDistanceMeasurement {
        override fun invoke(
            classifiers: CirKnownClassifiers, targetIndex: Int, from: CirClassOrTypeAliasType, to: CirEntityId
        ): CirTypeDistance = typeDistance(classifiers, targetIndex, from, to)
    }

    companion object {
        operator fun invoke(options: TypeCommonizer.Context): TypeDistanceMeasurement = when {
            options.enableBackwardsTypeAliasSubstitution && options.enableForwardTypeAliasSubstitution -> Full
            options.enableForwardTypeAliasSubstitution -> ForwardOnly
            else -> None
        }
    }
}
