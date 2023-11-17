/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal interface UsesLibraryFilterCachingService : Task {
    @get:Internal
    val libraryFilterCacheService: Property<LibraryFilterCachingService>
}

internal abstract class LibraryFilterCachingService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    internal data class LibraryFilterCacheKey(val dependency: File)

    private val cache = ConcurrentHashMap<LibraryFilterCacheKey, Boolean>()

    fun getOrCompute(key: LibraryFilterCacheKey, compute: (File) -> Boolean) = cache.computeIfAbsent(key) {
        compute(it.dependency)
    }

    override fun close() {
        cache.clear()
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<LibraryFilterCachingService> =
            project.rootProject.gradle.sharedServices.registerIfAbsent(
                "${LibraryFilterCachingService::class.java.canonicalName}_${LibraryFilterCachingService::class.java.classLoader.hashCode()}",
                LibraryFilterCachingService::class.java
            ) {}.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesLibraryFilterCachingService::class.java.name) {
                    project.tasks.withType<UsesLibraryFilterCachingService>().configureEach { task ->
                        task.usesService(serviceProvider)
                    }
                }
            }
    }
}