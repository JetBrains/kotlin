/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal interface UsesBuildSessionService : Task {
    @get:Internal
    val buildSessionService: Property<BuildSessionService>
}

/**
 * A Gradle [BuildService] to share the same [KotlinToolchain.BuildSession] instance between multiple tasks.
 */
internal abstract class BuildSessionService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val lock = ReentrantReadWriteLock()
    private var closed = false
    private val buildSessions = ConcurrentHashMap<List<File>, KotlinToolchains.BuildSession>()

    fun getOrCreateBuildSession(
        classloaderService: ClassLoadersCachingBuildService,
        compilerClasspath: List<File>,
    ): KotlinToolchains.BuildSession {
        lock.read {
            check(!closed) { "${BuildSessionService::class.java.simpleName} is already closed, cannot create new build sessions" }
            return buildSessions.computeIfAbsent(compilerClasspath) {
                val classLoader = classloaderService.getClassLoader(compilerClasspath, SharedApiClassesClassLoaderProvider)
                val compilationService = KotlinToolchains.loadImplementation(classLoader)
                compilationService.createBuildSession()
            }
        }
    }

    override fun close() {
        lock.write {
            check(!closed) { "${BuildSessionService::class.java.simpleName} is already closed" }
            closed = true
            buildSessions.values.forEach { it.close() }
            buildSessions.clear()
        }
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<BuildSessionService> =
            project.gradle.registerClassLoaderScopedBuildService(BuildSessionService::class).also { serviceProvider ->
                SingleActionPerProject.run(project, UsesBuildSessionService::class.java.name) {
                    project.tasks.withType<UsesBuildSessionService>().configureEach { task ->
                        task.usesService(serviceProvider)
                        task.buildSessionService.set(serviceProvider)
                    }
                }
            }

        fun getInstance(project: Project): BuildSessionService = registerIfAbsent(project).get()
    }
}
