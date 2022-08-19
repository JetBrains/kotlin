/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.*

internal fun applyKotlinTargetHierarchy(
    hierarchyDescriptor: KotlinTargetHierarchyDescriptor,
    targets: DomainObjectCollection<KotlinTarget>,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
) {
    targets
        .matching { target -> target.platformType != KotlinPlatformType.common }
        .all { target ->
            target.compilations.all { compilation ->
                val hierarchy = hierarchyDescriptor.buildKotlinTargetHierarchy(compilation)
                applyKotlinTargetHierarchy(hierarchy, compilation, sourceSets)
            }
        }
}

private fun applyKotlinTargetHierarchy(
    hierarchy: KotlinTargetHierarchy,
    compilation: KotlinCompilation<*>,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
): KotlinSourceSet? {
    val sharedSourceSetName = hierarchy.node.sharedSourceSetName(compilation) ?: return null
    val sharedSourceSet = sourceSets.maybeCreate(sharedSourceSetName)

    hierarchy.children
        .mapNotNull { childHierarchy -> applyKotlinTargetHierarchy(childHierarchy, compilation, sourceSets) }
        .forEach { childSourceSet -> childSourceSet.dependsOn(sharedSourceSet) }

    if (hierarchy.children.isEmpty()) {
        compilation.defaultSourceSet.dependsOn(sharedSourceSet)
    }

    return sharedSourceSet
}
