/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.*

internal interface UsesBuildIdProviderService : Task {
    @get:Internal
    val buildIdService: Property<BuildIdService>
}

internal abstract class BuildIdService : BuildService<BuildServiceParameters.None> {
    val buildId: UUID = UUID.randomUUID()

    companion object {
        fun registerIfAbsent(project: Project): Provider<BuildIdService> =
            project.gradle.registerClassLoaderScopedBuildService(BuildIdService::class).also { service ->
                project.tasks.withType<UsesBuildIdProviderService>().configureEach { task ->
                    task.usesService(service)
                }
            }
    }
}