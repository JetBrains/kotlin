/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future

@Deprecated(
    message = "Internal API, that will be removed in next major releases. Use KotlinCompilation.allAssociatedCompilations API",
    replaceWith = ReplaceWith("fromCompilation.allAssociatedCompilations.associateWith { it.allKotlinSourceSets }")
)
fun getSourceSetsFromAssociatedCompilations(fromCompilation: KotlinCompilation<*>): Map<KotlinCompilation<*>, Set<KotlinSourceSet>> =
    fromCompilation.sourceSetsByAssociatedCompilation()

private fun KotlinCompilation<*>.sourceSetsByAssociatedCompilation(): Map<KotlinCompilation<*>, Set<KotlinSourceSet>> =
    allAssociatedCompilations.associateWith { it.allKotlinSourceSets }

@Deprecated(
    "Internal API, that will be removed in next major releases. Use KotlinCompilation.allAssociatedCompilations API"
)
fun getVisibleSourceSetsFromAssociateCompilations(
    sourceSet: KotlinSourceSet
): List<KotlinSourceSet> = getVisibleSourceSetsFromAssociateCompilations(sourceSet.internal.compilations)

internal suspend fun InternalKotlinSourceSet.awaitVisibleSourceSetsFromAssociateCompilations(): List<KotlinSourceSet> =
    getVisibleSourceSetsFromAssociateCompilations(awaitPlatformCompilations())

internal val InternalKotlinSourceSet.visibleSourceSetsFromAssociateCompilationsFuture: Future<List<KotlinSourceSet>>
    get() = project.future { awaitVisibleSourceSetsFromAssociateCompilations() }

private fun getVisibleSourceSetsFromAssociateCompilations(
    participatesInCompilations: Set<KotlinCompilation<*>>
): List<KotlinSourceSet> {
    val visibleInCompilations = participatesInCompilations.map {
        val sourceSetsInAssociatedCompilations = it.sourceSetsByAssociatedCompilation()
        when (sourceSetsInAssociatedCompilations.size) {
            0 -> emptySet()
            1 -> sourceSetsInAssociatedCompilations.values.single()
            else -> mutableSetOf<KotlinSourceSet>().apply {
                for ((_, sourceSets) in sourceSetsInAssociatedCompilations) {
                    addAll(sourceSets)
                }
            }
        }
    }

    // Intersect the sets of source sets from the compilations:
    return when (visibleInCompilations.size) {
        0 -> emptySet()
        1 -> visibleInCompilations.single()
        else -> visibleInCompilations.reduce { intersection, kotlinSourceSets -> intersection intersect kotlinSourceSets }
    }.toList()
}
