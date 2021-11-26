/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import java.util.concurrent.ConcurrentHashMap

internal abstract class MayBeUpToDatePackageJsonTasksRegistry : BuildService<BuildServiceParameters.None> {
    private val mayBeUpToDateTasks = ConcurrentHashMap.newKeySet<String>()

    fun markForNpmDependenciesResolve(task: KotlinPackageJsonTask) {
        mayBeUpToDateTasks.add(task.path)
    }

    fun shouldResolveNpmDependenciesFor(taskPath: String) = taskPath in mayBeUpToDateTasks

    companion object {
        fun registerIfAbsent(project: Project): Provider<MayBeUpToDatePackageJsonTasksRegistry> =
            project.rootProject.gradle.sharedServices.registerIfAbsent(
                MayBeUpToDatePackageJsonTasksRegistry::class.qualifiedName,
                MayBeUpToDatePackageJsonTasksRegistry::class.java
            ) {}
    }
}