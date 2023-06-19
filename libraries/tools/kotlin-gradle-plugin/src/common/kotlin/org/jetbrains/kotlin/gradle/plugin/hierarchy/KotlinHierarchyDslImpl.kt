/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinHierarchyDsl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseRefinesEdges
import org.jetbrains.kotlin.gradle.plugin.mpp.internal

internal class KotlinHierarchyDslImpl(
    private val targets: DomainObjectCollection<KotlinTarget>,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) : KotlinHierarchyDsl {

    private val appliedTemplatesImpl = mutableSetOf<KotlinHierarchyTemplate>()

    val appliedTemplates get() = appliedTemplatesImpl.toSet()

    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
        if (!appliedTemplatesImpl.add(template)) return
        applyHierarchyTemplateToAllCompilations(template)
    }

    override fun applyHierarchyTemplate(template: KotlinHierarchyBuilder.Root.() -> Unit) {
        applyHierarchyTemplate(KotlinHierarchyTemplate(template))
    }

    override fun applyHierarchyTemplate(template: KotlinHierarchyTemplate, extension: KotlinHierarchyBuilder.Root.() -> Unit) {
        applyHierarchyTemplate(template.extend(extension))
    }

    /* Implementation */

    private fun applyHierarchyTemplateToAllCompilations(template: KotlinHierarchyTemplate) {
        targets.matching { target -> target.platformType != KotlinPlatformType.common }.all { target ->
            target.compilations.all { compilation ->
                target.project.kotlinPluginLifecycle.launch {
                    withRestrictedStages(KotlinPluginLifecycle.Stage.upTo(FinaliseRefinesEdges)) {
                        val hierarchy = template.buildHierarchy(compilation) ?: return@withRestrictedStages
                        applyKotlinHierarchy(hierarchy, compilation)
                    }
                }
            }
        }
    }

    private suspend fun applyKotlinHierarchy(hierarchy: KotlinHierarchy, compilation: KotlinCompilation<*>): KotlinSourceSet? {
        val sharedSourceSet = createSharedSourceSetOrNull(hierarchy.node, compilation)

        val childSourceSets =
            hierarchy.children.mapNotNull { childHierarchy -> applyKotlinHierarchy(childHierarchy, compilation) }

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
        node: KotlinHierarchy.Node, compilation: KotlinCompilation<*>,
    ): KotlinSourceSet? {
        val sharedSourceSetName = node.sharedSourceSetName(compilation) ?: return null
        return sourceSets.maybeCreate(sharedSourceSetName)
    }
}
