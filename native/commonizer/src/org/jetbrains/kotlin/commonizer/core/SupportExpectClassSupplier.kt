/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.konan.DefaultModulesProvider
import org.jetbrains.kotlin.commonizer.konan.NativeLibrariesToCommonize
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiersByModules
import org.jetbrains.kotlin.commonizer.mergedtree.loadAllClassifiers
import org.jetbrains.kotlin.commonizer.repository.CommonizerSupportLibraryRepository
import org.jetbrains.kotlin.commonizer.repository.Repository
import kotlin.collections.toMap

class SupportExpectClassSupplier internal constructor(
    private val targets: List<CommonizerTarget>,
    internal val supportLibraryModulesProvider: TargetDependent<ModulesProvider>,
) {
    internal constructor(targets: List<CommonizerTarget>, repository: CommonizerSupportLibraryRepository)
            : this(targets, repository.toModulesProvider(targets))

    companion object {
        fun empty() = SupportExpectClassSupplier(emptyList(), TargetDependent.empty())
    }

    private fun supportLibraryModulesProviderFor(target: CommonizerTarget) =
        supportLibraryModulesProvider[target]

    private val allClassifiersCache = mutableMapOf<CommonizerTarget, Map<CirEntityId, CirProvided.Classifier>>()

    private fun loadAllClassifiersAvailableFor(target: CommonizerTarget) =
        allClassifiersCache.getOrPut(target) { supportLibraryModulesProviderFor(target).loadAllClassifiers() }

    internal fun getProvidedClassifiers(target: CommonizerTarget): CirProvidedClassifiers =
        loadAllClassifiersAvailableFor(target).takeIf { it.isNotEmpty() }
            ?.let { CirProvidedClassifiersByModules(false, it) }
            ?: CirProvidedClassifiers.EMPTY

    private val allLeafClassifiersCache = mutableMapOf<CommonizerTarget, Map<CirEntityId, CirProvided.Classifier>>()

    private fun loadAllLeafClassifiersAvailableFor(target: CommonizerTarget) =
        allLeafClassifiersCache.getOrPut(target) {
            val allClassifiers = loadAllClassifiersAvailableFor(target)
            allClassifiers.filterValues { it.isLeafWithin(allClassifiers) }
        }

    private fun CirEntityId.fullyExpandedClassifierIfTypealiasOr(classifiers: CirProvidedClassifiers): CirEntityId? {
        val classifier = classifiers.classifier(this) ?: return null

        return when (classifier) {
            is CirProvided.Class -> this
            is CirProvided.TypeAlias -> classifier.underlyingType.toCirClassOrTypeAliasTypeOrNull(classifiers)?.classifierId
                ?.let { it.fullyExpandedClassifierIfTypealiasOr(classifiers) ?: it }
        }
    }

    internal fun CirEntityId.expandThroughDependencies(dependencies: CirProvidedClassifiers): CirEntityId {
        var result = this
        var next = fullyExpandedClassifierIfTypealiasOr(dependencies)

        // This loop only runs a single iteration on the actual support library because
        // leaf platform klibs contain copies of typealiases from shared source sets with
        // the proper typealias type `undelyingType`s.
        // In tests, however, leaf platform source sets do not, so an explicit traversal
        // of shared source sets' metadata is needed.
        while (next != null && next != result) {
            result = next
            next = fullyExpandedClassifierIfTypealiasOr(dependencies)
        }

        return result
    }

    private fun findSupportExpectClassFor(targetsToTypes: Map<CommonizerTarget, CirClassType>): CirEntityId? {
        // Why the flattening? Suppose we're commonizing `[(iosArm64, iosDeviceArm64)] to Long` with `[(watchosArm64, watchosArm32)] to Int`.
        // Even if there is some `expect class AppleSomething` that satisfies the expansions, we need to make sure it actually does by
        // checking the classifier against the dependencies of each target.
        // If we only look at the shared targets, we only see the shared dependencies, which may only provide us with
        // `actual typealias AppleSomething = IosSomething` and `actual typealias AppleSomething = WatchosSomething` respectively, but
        // neither of these is `Long` or `Int`.
        val flattenedTargetsToTypes = targetsToTypes.entries.flatMap { [target, type] -> target.allLeaves().map { it to type } }.toMap()

        // Even if we decided to put some Ios- and Watchos-specific `expect class` instead of the raw `Long` and `Int` on earlier stages,
        // this still wouldn't let us get away with only the immediate expansions: the choice of such classes is not unique, and we may
        // end up picking something that lacks a further Apple-specific counterpart.

        val targetsWithTheirDependencies = flattenedTargetsToTypes.mapValues { [target] -> getProvidedClassifiers(target) }
        val commonSupportClassifiers = loadAllLeafClassifiersAvailableFor(SharedCommonizerTarget(targetsToTypes.keys.allLeaves()))

        return commonSupportClassifiers.entries.find { [cirEntityId] ->
            targetsWithTheirDependencies.all { [target, dependencies] ->
                val supportExpectClassExpansion = cirEntityId.expandThroughDependencies(dependencies)
                val targetTypeExpansion = flattenedTargetsToTypes[target]?.classifierId?.expandThroughDependencies(dependencies)
                    ?: flattenedTargetsToTypes[target]?.classifierId
                supportExpectClassExpansion == targetTypeExpansion
            }
        }?.key
    }

    fun buildSupportExpectTypeFor(types: List<CirClassType>): CirEntityId? {
        val targetsToTypes = targets.zip(types).toMap()
        return findSupportExpectClassFor(targetsToTypes)
    }
}

/**
 * Chooses `expect class AppleType` over `actual typealias NativeType = AppleType`.
 */
private fun CirProvided.Classifier.isLeafWithin(classifiersForTargets: Map<CirEntityId, CirProvided.Classifier>): Boolean =
    when (this) {
        is CirProvided.Class -> true
        // Prevents losing `actual typealias NativeType = Int`.
        // Suppose we're commonizing `[(iosArm64, iosX64)] to Int` and `[(watchosArm64, watchosDeviceArm64)] to Int` within `appleMain`.
        // There may be no `expect class AppleSomething` which expands to `Int` on the individual targets, but there may very well be
        // an `actual typealias NativeSomething = Int` which we could use instead.
        is CirProvided.TypeAlias -> underlyingType.classifierId !in classifiersForTargets
    }

internal fun CommonizerSupportLibraryRepository.toModulesProvider(targets: Iterable<CommonizerTarget>) =
    TargetDependent(targets.withAllLeaves()) { target ->
        buildModulesProvider(this, target)
    }

private fun buildModulesProvider(supportLibraryRepository: Repository, target: CommonizerTarget): ModulesProvider =
    DefaultModulesProvider.create(NativeLibrariesToCommonize(target, supportLibraryRepository.getLibraries(target).toList()))

fun CirProvided.Classifier.dependencyClassifierToCir(
    classifierId: CirEntityId,
    outerType: CirClassType?,
    arguments: List<CirTypeProjection>,
    isMarkedNullable: Boolean,
    classifiers: CirProvidedClassifiers,
): CirClassOrTypeAliasType? = when (this) {
    is CirProvided.Class -> CirClassType.createInterned(
        classId = classifierId,
        outerType = outerType,
        arguments = arguments,
        isMarkedNullable = isMarkedNullable
    )

    is CirProvided.TypeAlias -> CirTypeAliasType.createInterned(
        typeAliasId = classifierId,
        arguments = arguments,
        isMarkedNullable = isMarkedNullable,
        underlyingType = underlyingType.toCirClassOrTypeAliasTypeOrNull(classifiers)
            ?.makeNullableIfNecessary(isMarkedNullable)
            ?.withParentArguments(arguments) ?: return null
    )
}
