/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.Companion.upTo
import org.jetbrains.kotlin.gradle.plugin.mpp.internal

internal fun applyKotlinTargetHierarchy(
    hierarchyDescriptor: KotlinTargetHierarchyDescriptor,
    targets: DomainObjectCollection<KotlinTarget>,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
) {
    targets
        .matching { target -> target.platformType != KotlinPlatformType.common }
        .all { target ->
            target.compilations.all forCompilation@{ compilation ->
                target.project.kotlinPluginLifecycle.launch {
                    withRestrictedStages(upTo(Stage.FinaliseRefinesEdges)) {
                        val hierarchy = hierarchyDescriptor.buildKotlinTargetHierarchy(compilation) ?: return@withRestrictedStages
                        applyKotlinTargetHierarchy(hierarchy, compilation, sourceSets)
                    }
                }
            }
        }
}

private suspend fun applyKotlinTargetHierarchy(
    hierarchy: KotlinTargetHierarchyTree,
    compilation: KotlinCompilation<*>,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
): KotlinSourceSet? {
    val sharedSourceSet = createSharedSourceSetOrNull(sourceSets, hierarchy.node, compilation)

    val childSourceSets = hierarchy.children
        .mapNotNull { childHierarchy -> applyKotlinTargetHierarchy(childHierarchy, compilation, sourceSets) }

    if (sharedSourceSet == null) return null

    if (hierarchy.children.isNotEmpty()) {
        childSourceSets.forEach { childSourceSet -> childSourceSet.dependsOn(sharedSourceSet) }
    } else {
        compilation.internal.kotlinSourceSets.forAll { compilationSourceSet ->
            compilationSourceSet.dependsOn(sharedSourceSet)
        }
    }

    return sharedSourceSet
}

private suspend fun createSharedSourceSetOrNull(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    node: KotlinTargetHierarchyTree.Node,
    compilation: KotlinCompilation<*>,
): KotlinSourceSet? {
    val sharedSourceSetName = node.sharedSourceSetName(compilation) ?: return null
    return sourceSets.maybeCreate(sharedSourceSetName)
}
