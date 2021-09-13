/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.commonizer.utils.safeCastValues
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class ClassOrTypeAliasTypeCommonizer(
    private val typeCommonizer: TypeCommonizer,
    private val classifiers: CirKnownClassifiers
) : NullableSingleInvocationCommonizer<CirClassOrTypeAliasType> {

    private val isMarkedNullableCommonizer = TypeNullabilityCommonizer(typeCommonizer.options)

    override fun invoke(values: List<CirClassOrTypeAliasType>): CirClassOrTypeAliasType? {
        if (values.isEmpty()) return null
        val expansions = values.map { it.expandedType() }
        val isMarkedNullable = isMarkedNullableCommonizer.commonize(expansions.map { it.isMarkedNullable }) ?: return null
        val arguments = TypeArgumentListCommonizer(typeCommonizer).commonize(values.map { it.arguments }) ?: return null
        val classifierId = selectClassifierId(values)
            ?: typeCommonizer.options.enableOptimisticNumberTypeCommonization.ifTrue {
                return OptimisticNumbersTypeCommonizer.commonize(expansions)
            } ?: return null

        val outerTypes = values.safeCastValues<CirClassOrTypeAliasType, CirClassType>()?.map { it.outerType }
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
                underlyingType = classifiers.commonDependencies
                    .toCirClassOrTypeAliasTypeOrNull(dependencyClassifier.underlyingType) ?: return null
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

    @Suppress("KotlinConstantConditions") // Improved readability
    private fun selectClassifierId(types: List<CirClassOrTypeAliasType>): CirEntityId? {
        types.singleDistinctValueOrNull { it.classifierId }?.let { return it }

        val forwardSubstitutionAllowed = typeCommonizer.options.enableForwardTypeAliasSubstitution
        val backwardsSubstitutionAllowed = typeCommonizer.options.enableBackwardsTypeAliasSubstitution

        /* No substitution allowed in any direction */
        if (!forwardSubstitutionAllowed && !backwardsSubstitutionAllowed) {
            return null
        }

        val commonId = types.singleDistinctValueOrNull {
            classifiers.commonClassifierIdResolver.findCommonId(it.classifierId)
        } ?: return null

        val candidates = when {
            forwardSubstitutionAllowed && backwardsSubstitutionAllowed -> commonId.aliases

            /* Only forward substitutions allowed -> Only substitute with any underlying type */
            forwardSubstitutionAllowed -> commonId.aliases.filter { candidate ->
                types.all { type -> candidate == type.classifierId || type.isAnyUnderlyingClassifier(candidate) }
            }

            /* Only backward substitutions allowed -> Only substitute with any typealias pointing to this types */
            backwardsSubstitutionAllowed -> commonId.aliases.filter { candidate ->
                types.none { type -> type.isAnyUnderlyingClassifier(candidate) }
            }

            else -> error("Failed to select classifierId: Unexpected when branch")
        }

        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        return candidates.maxByOrNull { candidate -> types.count { it.classifierId == candidate } }!!
    }

    private fun CirClassOrTypeAliasType.isAnyUnderlyingClassifier(classifierId: CirEntityId): Boolean {
        return when (this) {
            is CirClassType -> false
            is CirTypeAliasType -> this.isAnyUnderlyingClassifier(classifierId)
        }
    }

    private fun CirTypeAliasType.isAnyUnderlyingClassifier(classifierId: CirEntityId): Boolean {
        return underlyingType.classifierId == classifierId || underlyingType.isAnyUnderlyingClassifier(classifierId)
    }
}
