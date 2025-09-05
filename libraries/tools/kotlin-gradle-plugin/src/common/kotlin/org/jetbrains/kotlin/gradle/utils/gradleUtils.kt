/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import kotlin.reflect.KClass

internal val Project.compositeBuildRootGradle: Gradle get() = generateSequence(project.gradle) { it.parent }.last()
internal val Project.compositeBuildRootProject: Project get() = compositeBuildRootGradle.rootProject

/**
 * Run block function on root project of the root build in composite build only when a root project becomes available
 */
internal fun Project.compositeBuildRootProject(block: (Project) -> Unit) = compositeBuildRootGradle.rootProject(block)

internal fun <T : BuildService<P>, P : BuildServiceParameters> Gradle.registerClassLoaderScopedBuildService(
    serviceClass: KClass<T>,
    configureAction: Action<BuildServiceSpec<P>> = Action { },
): Provider<T> {
    val serviceName = "${serviceClass.simpleName}_${serviceClass.java.classLoader.hashCode()}"
    return sharedServices.registerIfAbsent(serviceName, serviceClass.java, configureAction)
}

/**
 * Will return the [ProjectComponentIdentifier.getBuild] if the component
 * represents a project.
 */
internal val ComponentIdentifier.buildOrNull: BuildIdentifier?
    get() = (this as? ProjectComponentIdentifier)?.build

/**
 * Returns the associated 'projectPath' if the component represents a project
 * null, otherwise
 */
internal val ComponentIdentifier.projectPathOrNull: String?
    get() = (this as? ProjectComponentIdentifier)?.projectPath