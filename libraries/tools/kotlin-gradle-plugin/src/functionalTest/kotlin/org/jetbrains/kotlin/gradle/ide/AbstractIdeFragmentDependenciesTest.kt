/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.ide

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kpmExtension
import org.jetbrains.kotlin.gradle.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.plugin.ide.*
import org.jetbrains.kotlin.gradle.kpm.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.kpm.KpmExtension
import org.jetbrains.kotlin.gradle.kpm.currentBuildId

@OptIn(InternalIdeApi::class)
abstract class AbstractIdeFragmentDependenciesTest {
    val rootProject = ProjectBuilder.builder().build() as ProjectInternal

    private val ideFragmentDependencyResolvers: MutableMap<Project, IdeFragmentDependencyResolver> = mutableMapOf()

    private fun getIdeFragmentDependencyResolver(project: Project) = ideFragmentDependencyResolvers.getOrPut(project) {
        IdeFragmentDependencyResolver.create(project)
    }

    internal fun resolveIdeDependenciesList(fragment: KotlinGradleFragment): List<IdeFragmentDependency> {
        return getIdeFragmentDependencyResolver(fragment.project).resolveDependencies(fragment)
    }

    internal fun resolveIdeDependenciesSet(fragment: KotlinGradleFragment): Set<IdeFragmentDependency> {
        return resolveIdeDependenciesList(fragment).toSet()
    }
}

internal inline fun AbstractIdeFragmentDependenciesTest.createKpmProject(
    name: String, configure: KpmExtension.() -> Unit
): KpmExtension {
    val project = ProjectBuilder.builder().withName(name).withParent(rootProject).build() as ProjectInternal
    project.applyKpmPlugin()
    project.kpmExtension.configure()
    return project.kpmExtension
}

internal inline fun AbstractIdeFragmentDependenciesTest.createMultiplatformProject(
    name: String, configure: KotlinMultiplatformExtension.() -> Unit
): KotlinMultiplatformExtension {
    val project = ProjectBuilder.builder().withName(name).withParent(rootProject).build() as ProjectInternal
    project.applyMultiplatformPlugin()
    project.multiplatformExtension.configure()
    return project.multiplatformExtension
}

internal fun ideLocalSourceDependenciesListOf(vararg fragment: KotlinGradleFragment):
        List<IdeLocalSourceFragmentDependency> = listOf(*fragment).map { it.toIdeFragmentDependency() }

internal fun ideLocalSourceDependenciesSetOf(vararg fragment: KotlinGradleFragment):
        Set<IdeLocalSourceFragmentDependency> = ideLocalSourceDependenciesListOf(*fragment).toSet()


fun KotlinGradleFragment.toIdeFragmentDependency(): IdeLocalSourceFragmentDependency {
    return IdeLocalSourceFragmentDependency(
        buildId = project.currentBuildId(),
        projectPath = project.path,
        projectName = project.name,
        kotlinModuleName = containingModule.name,
        kotlinFragmentName = name
    )
}

