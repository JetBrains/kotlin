/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.cir.CirClassType.Companion.copyInterned
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType.Companion.copyInterned
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirTypeSubstitutor
import org.jetbrains.kotlin.commonizer.tree.CirTreeTypeAlias

internal class AliasedTypeSubstitutor(
    private val commonDependencies: CirProvidedClassifiers,
    private val classifierIndices: TargetDependent<CirClassifierIndex>
) : CirTypeSubstitutor {

    override fun substitute(targetIndex: Int, type: CirType): CirType {
        return when (type) {
            is CirFlexibleType -> type
            is CirTypeParameterType -> type
            is CirClassOrTypeAliasType -> substituteClassOrTypeAliasType(targetIndex, type)
        }
    }

    private fun substituteTypeProjection(targetIndex: Int, projection: CirTypeProjection): CirTypeProjection {
        return when (projection) {
            is CirRegularTypeProjection -> {
                val newType = substitute(targetIndex, projection.type)
                if (newType != projection.type) projection.copy(type = newType) else projection
            }
            is CirStarTypeProjection -> projection
        }
    }

    private fun substituteClassOrTypeAliasType(targetIndex: Int, type: CirClassOrTypeAliasType): CirClassOrTypeAliasType {
        /*
        Classifier id is available in all platforms or is provided by common dependencies.
        The classifier itself does not require substitution, but type arguments potentially do!
         */
        if (isCommon(type.classifierId)) {
            val newArguments = type.arguments.map { argument -> substituteTypeProjection(targetIndex, argument) }
            return if (newArguments != type.arguments) when (type) {
                is CirTypeAliasType -> type.copyInterned(arguments = newArguments)
                is CirClassType -> type.copyInterned(arguments = newArguments)
            } else type
        }

        /**
         * Yet, we do not have any evidence that it is worth trying to substitute types with arguments when the type itself is
         * not common at all. This substitutor could try to substitute the type & it's arguments.
         *
         * Without any evidence of libraries that would benefit from this higher effort, we will just skip this case.
         */
        if (type.arguments.isNotEmpty()) {
            return type
        }

        return when (type) {
            is CirClassType -> substituteClassType(targetIndex, type)
            is CirTypeAliasType -> substituteClassType(targetIndex, type.unabbreviate())
        }
    }

    /**
     * Tries to find a suitable type alias pointing to the given class [type] which is available in 'common'
     * This function does *not* support types with arguments!
     */
    private fun substituteClassType(targetIndex: Int, type: CirClassType): CirClassOrTypeAliasType {
        if (type.arguments.isNotEmpty()) return type
        if (type.outerType != null) return type
        if (isCommon(type.classifierId)) return type
        val classifierIndex = classifierIndices[targetIndex]

        val typeAliases = classifierIndex.findTypeAliasesWithUnderlyingType(type.classifierId)
        val commonTypeAlias = findSuitableCommonTypeAlias(classifierIndex, typeAliases) ?: return type

        return CirTypeAliasType.createInterned(
            typeAliasId = commonTypeAlias.id,
            underlyingType = commonTypeAlias.typeAlias.underlyingType,
            arguments = emptyList(),
            isMarkedNullable = type.isMarkedNullable
        )
    }

    private tailrec fun findSuitableCommonTypeAlias(
        index: CirClassifierIndex,
        typeAliases: List<CirTreeTypeAlias>
    ): CirTreeTypeAlias? {
        if (typeAliases.isEmpty()) return null

        val commonTypeAlias = typeAliases.firstOrNull { (id, typeAlias) -> typeAlias.typeParameters.isEmpty() && isCommon(id) }
        if (commonTypeAlias != null) {
            return commonTypeAlias
        }

        return findSuitableCommonTypeAlias(
            index, typeAliases.flatMap { (id, _) -> index.findTypeAliasesWithUnderlyingType(id) }
        )
    }

    private fun isCommon(id: CirEntityId): Boolean =
        commonDependencies.hasClassifier(id) || classifierIndices.all { index -> id in index.allClassifierIds }
}

