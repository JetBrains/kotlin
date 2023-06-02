/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinSourceSetTreeDependsOnMismatch
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object KotlinSourceSetTreeDependsOnMismatchChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val sourceSets = this.multiplatformExtension
            ?.awaitSourceSets()
            // Ignoring Android source sets
            ?.filter { it.androidSourceSetInfoOrNull == null }
            ?: return

        // A "good" source set is part of only single Source Set Tree
        val goodSourceSets = mutableMapOf<KotlinSourceSet, SourceSetTree?>()
        // A "bad" source set is part of >=2 Source Set Trees
        val badSourceSets = mutableMapOf<KotlinSourceSet, List<SourceSetTree?>>()

        val reverseSourceSetDependencies = mutableMapOf<KotlinSourceSet, MutableSet<KotlinSourceSet>>()
        fun KotlinSourceSet.addReverseDependencyTo(that: KotlinSourceSet) =
            reverseSourceSetDependencies.getOrPut(this) { mutableSetOf() }.add(that)

        for (sourceSet in sourceSets) {
            sourceSet.dependsOn.forEach { it.addReverseDependencyTo(sourceSet) }

            val platformCompilations = sourceSet.internal.awaitPlatformCompilations()
            val distinctSourceSetTrees = platformCompilations.map { SourceSetTree.orNull(it) }.distinct()

            val totalDistinctSourceSetTrees = distinctSourceSetTrees.size

            @Suppress("KotlinConstantConditions")
            when {
                totalDistinctSourceSetTrees > 1 -> badSourceSets[sourceSet] = distinctSourceSetTrees
                totalDistinctSourceSetTrees == 1 -> goodSourceSets[sourceSet] = distinctSourceSetTrees.single()
                // case when source set has no platform compilation and thus its totalDistinctSourceSetTrees == 0
                // is covered by [UnusedSourceSetsChecker]
                totalDistinctSourceSetTrees == 0 -> continue
            }
        }

        for ((badSourceSet, sourceSetTrees) in badSourceSets) {
            val dependents = reverseSourceSetDependencies[badSourceSet].orEmpty()

            // check if any of its dependents is also a bad source set then skip it
            // For example if nativeTest depends on nativeMain,
            // then transitively commonMain would have incorrect source set tree as well, but it is not a root cause.
            // NB: user can still add commonTest to commonMain dependency directly but this case will not be reported
            // until "dependent source sets relations is fixed
            if (dependents.any { it in badSourceSets }) continue

            // Heuristic: pick two source sets with different SourceSetTree
            // Theoretically there could be a lot of invalid pairs,
            // but it should be enough to report only 1 of them as it should cover the majority of cases.
            // NB: it is safe to do destruction on two elements as per definition of "bad" source set.
            val (sourceSetTreeA, sourceSetTreeB) = sourceSetTrees
            val sourceSetA = dependents.firstOrNull { it in goodSourceSets && goodSourceSets[it] == sourceSetTreeA }
            val sourceSetB = dependents.firstOrNull { it in goodSourceSets && goodSourceSets[it] == sourceSetTreeB }

            if (sourceSetA == null || sourceSetB == null) {
                val goodSourceSet = sourceSetA ?: sourceSetB
                if (goodSourceSet == null) {
                    project.logger.warn("Can't identify why kotlin source set '${badSourceSet.name}' has incorrect dependsOn relation")
                } else {
                    collector.report(
                        project,
                        KotlinSourceSetTreeDependsOnMismatch(goodSourceSet.name, badSourceSet.name)
                    )
                }
            } else {
                collector.report(
                    project,
                    KotlinSourceSetTreeDependsOnMismatch(sourceSetA.name, sourceSetB.name, badSourceSet.name)
                )
            }
        }
    }
}