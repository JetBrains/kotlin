/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.commonizer.utils.safeCastValues
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ClassOrTypeAliasTypeCommonizer(
    private val typeCommonizer: TypeCommonizer,
    private val classifiers: CirKnownClassifiers
) : NullableSingleInvocationCommonizer<CirClassOrTypeAliasType> {

    private val isMarkedNullableCommonizer = TypeNullabilityCommonizer(typeCommonizer.options)

    override fun invoke(values: List<CirClassOrTypeAliasType>): CirClassOrTypeAliasType? {
        if (values.isEmpty()) return null
        val expansions = values.map { it.expandedType() }
        val isMarkedNullable = isMarkedNullableCommonizer.commonize(expansions.map { it.isMarkedNullable }) ?: return null

        val types = substituteTypesIfNecessary(values) ?: typeCommonizer.options.enableOptimisticNumberTypeCommonization.ifTrue {
            return OptimisticNumbersTypeCommonizer.commonize(expansions)
        } ?: return null

        val classifierId = types.singleDistinctValueOrNull { it.classifierId } ?: return null

        val arguments = TypeArgumentListCommonizer(typeCommonizer).commonize(types.map { it.arguments }) ?: return null

        val outerTypes = types.safeCastValues<CirClassOrTypeAliasType, CirClassType>()?.map { it.outerType }
        val outerType = when {
            outerTypes == null -> null
            outerTypes.all { it == null } -> null
            outerTypes.any { it == null } -> return null
            else -> invoke(outerTypes.map { checkNotNull(it) }) as? CirClassType ?: return null
        }

        if (classifierId.packageName.isUnderKotlinNativeSyntheticPackages) {
            return CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                visibility = Visibilities.Public,
                isMarkedNullable = isMarkedNullable
            )
        }

        when (val dependencyClassifier = classifiers.commonDependencies.classifier(classifierId)) {
            is CirProvided.Class -> return CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                visibility = Visibilities.Public,
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

        val commonizedClassifier = classifiers.commonizedNodes.classNode(classifierId)?.commonDeclaration?.invoke()
            ?: classifiers.commonizedNodes.typeAliasNode(classifierId)?.commonDeclaration?.invoke()

        return when (commonizedClassifier) {
            is CirClass -> CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                visibility = Visibilities.Public,
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
        // Not necessary
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
            ?.withParentArguments(sourceType.arguments, sourceType.isMarkedNullable)
    }

    private fun backwardsSubstitute(
        sourceType: CirClassOrTypeAliasType,
        destinationTypeAliasId: CirEntityId,
        destinationTypeAlias: CirTypeAlias
    ): CirTypeAliasType? {
        // Limitation: No backwards substitution with arguments
        if (sourceType.arguments.isNotEmpty()) return null
        if (destinationTypeAlias.typeParameters.isNotEmpty()) return null

        // Check if any 'backward' type has arguments
        generateSequence(destinationTypeAlias.underlyingType) { type -> type.safeAs<CirTypeAliasType>()?.underlyingType }
            .takeWhile { type -> type.classifierId != sourceType.classifierId }
            .forEach { type -> if (type.arguments.isNotEmpty()) return null } // limitation!

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
        // Limitation: No backwards substitution with arguments
        if (sourceType.arguments.isNotEmpty()) return null
        if (destinationTypeAlias.typeParameters.isNotEmpty()) return null

        val providedClassifiers = CirProvidedClassifiers.of(classifiers.commonDependencies, classifiers.targetDependencies[targetIndex])

        generateSequence(destinationTypeAlias.underlyingType) next@{ type ->
            val typeAliasType = type as? CirProvided.TypeAliasType ?: return@next null
            val typeAlias = providedClassifiers.classifier(typeAliasType.classifierId) as? CirProvided.TypeAlias ?: return@next null
            typeAlias.underlyingType
        }.takeWhile { type -> type.classifierId != sourceType.classifierId }
            .forEach { type -> if (type.arguments.isNotEmpty()) return null }

        return CirTypeAliasType.createInterned(
            destinationTypeAliasId,
            underlyingType = destinationTypeAlias.underlyingType.toCirClassOrTypeAliasTypeOrNull(providedClassifiers) ?: return null,
            arguments = emptyList(),
            isMarkedNullable = sourceType.isMarkedNullable
        )
    }

    private fun selectSubstitutionClassifierId(types: List<CirClassOrTypeAliasType>): CirEntityId? {
        val forwardSubstitutionAllowed = typeCommonizer.options.enableForwardTypeAliasSubstitution
        val backwardsSubstitutionAllowed = typeCommonizer.options.enableBackwardsTypeAliasSubstitution

        /* No substitution allowed in any direction */
        if (!forwardSubstitutionAllowed && !backwardsSubstitutionAllowed) {
            return null
        }

        val commonId = types.singleDistinctValueOrNull {
            classifiers.commonClassifierIdResolver.resolveId(it.classifierId)
        } ?: return null

        val typeSubstitutionCandidates = resolveTypeSubstitutionCandidates(classifiers, commonId, types)
            .filter { typeSubstitutionCandidate ->
                assert(typeSubstitutionCandidate.typeDistance.isZero.not()) { "Expected no zero typeDistance" }
                if (typeSubstitutionCandidate.typeDistance.isNotReachable) return@filter false
                if (typeSubstitutionCandidate.typeDistance.isNegative && !backwardsSubstitutionAllowed) return@filter false
                if (typeSubstitutionCandidate.typeDistance.isPositive && !forwardSubstitutionAllowed) return@filter false
                true
            }

        return typeSubstitutionCandidates.minByOrNull { it.typeDistance.penalty }?.id
    }
}

private class TypeSubstitutionCandidate(
    val id: CirEntityId,
    val typeDistance: CirTypeDistance
)

private fun resolveTypeSubstitutionCandidates(
    classifiers: CirKnownClassifiers, commonId: CirCommonClassifierId, types: List<CirClassOrTypeAliasType>
): List<TypeSubstitutionCandidate> {
    return commonId.aliases.mapNotNull mapCandidateId@{ candidateId ->
        val typeDistances = types.mapIndexed { targetIndex, type -> typeDistance(classifiers, targetIndex, type, candidateId) }
        if (typeDistances.any { it.isNotReachable }) return@mapCandidateId null
        TypeSubstitutionCandidate(
            id = candidateId,
            typeDistance = checkNotNull(typeDistances.maxByOrNull { it.penalty })
        )
    }
}
