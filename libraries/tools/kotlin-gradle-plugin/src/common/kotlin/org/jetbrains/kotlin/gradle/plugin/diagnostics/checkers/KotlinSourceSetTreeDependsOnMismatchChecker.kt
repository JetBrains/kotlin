/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinSourceSetDependsOnDefaultCompilationSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinSourceSetTreeDependsOnMismatch
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object KotlinSourceSetTreeDependsOnMismatchChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val sourceSets = this.multiplatformExtension?.awaitSourceSets() ?: return

        // A "good" source set is part of only single Source Set Tree
        val goodSourceSets = mutableMapOf<KotlinSourceSet, KotlinSourceSetTree?>()
        // A "bad" source set is part of >=2 Source Set Trees
        val badSourceSets = mutableMapOf<KotlinSourceSet, Set<KotlinSourceSetTree?>>()
        // A "leaf" source set is a source set with known Source Set Tree by default
        val leafSourceSets = multiplatformExtension
            .awaitTargets()
            .filter { it !is KotlinMetadataTarget }
            .flatMap { target -> target.compilations.map { it.defaultSourceSet to KotlinSourceSetTree.orNull(it) } }
            .toMap()

        val reverseSourceSetDependencies = mutableMapOf<KotlinSourceSet, MutableSet<KotlinSourceSet>>()
        fun KotlinSourceSet.addReverseDependencyTo(that: KotlinSourceSet) =
            reverseSourceSetDependencies.getOrPut(this) { mutableSetOf() }.add(that)

        for (sourceSet in sourceSets) {
            sourceSet.dependsOn.forEach { it.addReverseDependencyTo(sourceSet) }

            val platformCompilations = sourceSet.internal.awaitPlatformCompilations()
            val distinctSourceSetTrees = platformCompilations.map { KotlinSourceSetTree.orNull(it) }.toSet()

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

        reportAllDependentsOfLeafSourceSets(collector, leafSourceSets.keys, reverseSourceSetDependencies)

        for ((badSourceSet, _) in badSourceSets) {
            val dependents = reverseSourceSetDependencies[badSourceSet].orEmpty()

            // check if any of [badSourceSet] dependents is also a bad source set then skip it
            // For example if nativeTest depends on nativeMain,
            // then transitively commonMain would have incorrect source set tree as well, but it is not a root cause.
            // NB: user can still add commonTest to commonMain dependency directly but this case will not be reported
            // until underlying dependent source sets relations are fixed
            if (dependents.any { it in badSourceSets }) continue

            if (badSourceSet in leafSourceSets) {
                // It should be reported in [reportAllDependentsOfLeafSourceSets]
                continue
            }

            // Heuristic "White Crow": If among dependents there is only one source set with different Source Set Tree then report it
            // i.e.
            //                commonMain
            //                    |
            //        +-----------+------------+
            //        |           |            |
            //    (jvmMain)  (nativeMain)  (nativeTest!?)
            // jvmMain and nativeMain make a group of 'main' Source Set Tree,
            // but nativeTest is the only one from group of 'test' Source Set Tree
            // therefore dependency from nativeTest to commonMain is incorrect
            // A bad scenario for this heuristic is when in Bamboo Source Set Structure (source set with single dependent)
            // user adds two or more depends on edges from other Source Set Tree.
            // i.e.
            //                commonMain
            //                    |
            //        +-----------+---------------+
            //        |           |               |
            //    (nativeMain) (appleTest!?)  (nativeTest!?)
            // In this case dependency edge from nativeMain will be considered incorrect
            val dependentsBySourceSetTree = dependents.groupBy {
                when (it) {
                    in goodSourceSets -> goodSourceSets[it]
                    in leafSourceSets -> leafSourceSets[it]
                    else -> null
                }
            }
            if (reportSingleSourceSetWithDifferentSourceSetTree(collector, badSourceSet, dependentsBySourceSetTree)) continue

            // If there are more than one Source Sets with different Source Set Trees then we can't
            // identify which group is incorrect, therefore we should report all of them
            reportAllIncorrectSourceSetEdges(collector, badSourceSet, dependentsBySourceSetTree)
        }
    }

    private fun KotlinGradleProjectCheckerContext.reportAllDependentsOfLeafSourceSets(
        collector: KotlinToolingDiagnosticsCollector,
        leafSourceSets: Set<KotlinSourceSet>,
        reverseSourceSetDependencies: Map<KotlinSourceSet, Set<KotlinSourceSet>>,
    ) {
        for (leafSourceSet in leafSourceSets) {
            val dependents = reverseSourceSetDependencies[leafSourceSet].orEmpty()
            for (dependent in dependents) {
                collector.report(
                    project,
                    KotlinSourceSetDependsOnDefaultCompilationSourceSet(dependent.name, leafSourceSet.name)
                )
            }
        }
    }

    private fun KotlinGradleProjectCheckerContext.reportSingleSourceSetWithDifferentSourceSetTree(
        collector: KotlinToolingDiagnosticsCollector,
        badSourceSet: KotlinSourceSet,
        dependentsBySourceSetTree: Map<KotlinSourceSetTree?, List<KotlinSourceSet>>
    ): Boolean {
        val singleDependee = dependentsBySourceSetTree
            .values
            .singleOrNull { it.size == 1 }
            ?.single()
            ?: return false

        collector.report(project, KotlinSourceSetTreeDependsOnMismatch(singleDependee.name, badSourceSet.name))
        return true
    }

    private fun KotlinGradleProjectCheckerContext.reportAllIncorrectSourceSetEdges(
        collector: KotlinToolingDiagnosticsCollector,
        badSourceSet: KotlinSourceSet,
        dependentsBySourceSetTree: Map<KotlinSourceSetTree?, List<KotlinSourceSet>>,
    ) {
        val dependentsGroup = dependentsBySourceSetTree
            .mapKeys { it.key?.name ?: "null" }
            .mapValues { it.value.map(KotlinSourceSet::getName) }

        collector.report(project, KotlinSourceSetTreeDependsOnMismatch(dependentsGroup, badSourceSet.name))
    }
}