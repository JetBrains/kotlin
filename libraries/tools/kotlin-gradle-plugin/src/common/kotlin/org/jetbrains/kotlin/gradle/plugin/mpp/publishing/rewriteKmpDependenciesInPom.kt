/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty

private val Project.pomDependenciesRewriterMap: MutableMap<KotlinTargetComponent, PomDependenciesRewriter>
        by projectStoredProperty { mutableMapOf() }

private fun Project.pomDependenciesRewriterForTargetComponent(kotlinComponent: KotlinTargetComponent): PomDependenciesRewriter =
    pomDependenciesRewriterMap.getOrPut(kotlinComponent) {
        project.createDefaultPomDependenciesRewriterForTargetComponent(kotlinComponent)
    }

internal fun Project.rewriteKmpDependenciesInPomForTargetPublication(
    component: KotlinTargetComponent,
    publication: MavenPublication,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>? = null
) {
    val pomRewriter = pomDependenciesRewriterForTargetComponent(component)
    rewriteDependenciesInPom(pomRewriter, publication, includeOnlySpecifiedDependencies)
}

internal fun Project.rewriteDependenciesInPom(
    pomRewriter: PomDependenciesRewriter,
    publication: MavenPublication,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>?,
) {
    val pom = publication.pom
    if (pomRewriter.taskDependencies != null) {
        addTaskDependenciesToPomGenerator(publication, pomRewriter.taskDependencies!!)
    }

    val shouldRewritePomDependencies =
        project.provider { PropertiesProvider(project).keepMppDependenciesIntactInPoms != true }

    rewritePomXml(pom, shouldRewritePomDependencies, pomRewriter, includeOnlySpecifiedDependencies)
}

// This function is guard for lambda that is passed to `withXml`.
// If that lambda is created somewhere else, then it may violate Configuration Cache invariants,
// so please keep it AS IS!
private fun rewritePomXml(
    pom: MavenPom,
    shouldRewritePomDependencies: Provider<Boolean>,
    pomRewriter: PomDependenciesRewriter,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>?,
) {
    pom.withXml { xml ->
        if (shouldRewritePomDependencies.get())
            pomRewriter.rewritePomMppDependenciesToActualTargetModules(xml, includeOnlySpecifiedDependencies)
    }
}