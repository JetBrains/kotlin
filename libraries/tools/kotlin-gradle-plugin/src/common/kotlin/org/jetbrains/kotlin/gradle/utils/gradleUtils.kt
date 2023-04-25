/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import kotlin.reflect.KClass

val Gradle.projectCacheDir
    get() = startParameter.projectCacheDir ?: this.rootProject.projectDir.resolve(".gradle")

internal val Project.compositeBuildRootProject: Project get() = generateSequence(project.gradle) { it.parent }.last().rootProject

internal fun <T : BuildService<P>, P : BuildServiceParameters> Gradle.registerClassLoaderScopedBuildService(
    serviceClass: KClass<T>,
    configureAction: Action<BuildServiceSpec<P>> = Action { },
): Provider<T> {
    val serviceName = "${serviceClass.simpleName}_${serviceClass.java.classLoader.hashCode()}"
    return sharedServices.registerIfAbsent(serviceName, serviceClass.java, configureAction)
}
