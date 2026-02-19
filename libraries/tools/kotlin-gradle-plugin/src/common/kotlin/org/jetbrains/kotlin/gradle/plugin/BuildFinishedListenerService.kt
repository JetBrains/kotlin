/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal interface UsesBuildFinishedListenerService : Task {
    @get:Internal
    val buildFinishedListenerService: Property<BuildFinishedListenerService>
}

/**
 * A Gradle [BuildService] to register arbitrary actions that should run when the build completes.
 * Note that this is not guaranteed to be the exact build finish. Gradle may [close] the service once all tasks that use it are complete.
 *
 * The service is thread-safe and preserves the order in which the actions are registered.
 *
 * @see [Task.usesService]
 */
internal abstract class BuildFinishedListenerService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val actionsOnClose = ConcurrentLinkedQueue<() -> Unit>()
    private val keys = ConcurrentHashMap.newKeySet<String>()
    private var closing = false
    private val lock = ReentrantReadWriteLock()

    /**
     * Acquires [read] lock to prevent from running simultaneously with [close],
     * but allowing to have concurrent [onClose] and [onCloseOnceByKey]
     */
    fun onClose(action: () -> Unit) {
        lock.read {
            check(!closing) { "${BuildFinishedListenerService::class.java.simpleName} is already closed, cannot register new actions" }
            actionsOnClose.add(action)
        }
    }

    /**
     * Acquires [read] lock to prevent from running simultaneously with [close],
     * but allowing to have concurrent [onClose] and [onCloseOnceByKey]
     */
    fun onCloseOnceByKey(key: String, action: () -> Unit) {
        lock.read {
            check(!closing) { "${BuildFinishedListenerService::class.java.simpleName} is already closed, cannot register new actions" }
            if (keys.add(key)) {
                actionsOnClose.add(action)
            }
        }
    }

    /**
     * Acquires [write] lock
     * for awaiting all running [onClose] or [onCloseOnceByKey] operations to finish
     * to avoid having actions that are registered but never executed.
     */
    override fun close() {
        lock.write {
            check(!closing) { "${BuildFinishedListenerService::class.java.simpleName} is already closed" }
            closing = true
            while (actionsOnClose.isNotEmpty()) {
                actionsOnClose.poll()?.invoke()
            }
        }
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<BuildFinishedListenerService> =
            project.gradle.registerClassLoaderScopedBuildService(BuildFinishedListenerService::class).also { serviceProvider ->
                SingleActionPerProject.run(project, UsesBuildFinishedListenerService::class.java.name) {
                    project.tasks.withType<UsesBuildFinishedListenerService>().configureEach { task ->
                        task.usesService(serviceProvider)
                        task.buildFinishedListenerService.set(serviceProvider)
                    }
                }
            }

        fun getInstance(project: Project): BuildFinishedListenerService = registerIfAbsent(project).get()
    }
}
