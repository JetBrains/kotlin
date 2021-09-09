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
        val classifierId = selectClassifierId(values) ?: return null

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


        when (commonizedClassifier) {
            is CirClass -> return CirClassType.createInterned(
                classId = classifierId,
                outerType = outerType,
                arguments = arguments,
                visibility = Visibilities.Public,
                isMarkedNullable = isMarkedNullable
            )

            is CirTypeAlias -> return CirTypeAliasType.createInterned(
                typeAliasId = classifierId,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable,
                underlyingType = computeSuitableUnderlyingType(
                    classifiers, typeCommonizer, commonizedClassifier.underlyingType
                )?.makeNullableIfNecessary(isMarkedNullable) ?: return null
            )

            else -> Unit
        }

        return null
    }

    private fun selectClassifierId(types: List<CirClassOrTypeAliasType>): CirEntityId? {
        types.singleDistinctValueOrNull { it.classifierId }?.let { return it }

        if (typeCommonizer.options.enableTypeAliasSubstitution) {
            val commonId = types.singleDistinctValueOrNull {
                classifiers.commonClassifierIdResolver.findCommonId(it.classifierId)
            } ?: return null

            return commonId.aliases.maxByOrNull { candidate -> types.count { it.classifierId == candidate } }!!
        } else return null
    }
}

internal tailrec fun CirClassOrTypeAliasType.expandedType(): CirClassType = when (this) {
    is CirClassType -> this
    is CirTypeAliasType -> this.underlyingType.expandedType()
}
