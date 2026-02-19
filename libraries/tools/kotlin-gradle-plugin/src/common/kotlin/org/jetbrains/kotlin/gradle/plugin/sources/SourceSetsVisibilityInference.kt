/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun getSourceSetsFromAssociatedCompilations(fromCompilation: KotlinCompilation<*>): Map<KotlinCompilation<*>, Set<KotlinSourceSet>> =
    fromCompilation.allAssociatedCompilations.associate { it to it.allKotlinSourceSets }

fun getVisibleSourceSetsFromAssociateCompilations(
    sourceSet: KotlinSourceSet
): List<KotlinSourceSet> = getVisibleSourceSetsFromAssociateCompilations(sourceSet.internal.compilations)

internal fun getVisibleSourceSetsFromAssociateCompilations(
    participatesInCompilations: Set<KotlinCompilation<*>>
): List<KotlinSourceSet> {
    val visibleInCompilations = participatesInCompilations.map {
        val sourceSetsInAssociatedCompilations = getSourceSetsFromAssociatedCompilations(it)
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
