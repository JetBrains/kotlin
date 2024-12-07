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

internal interface UsesBuildFinishedListenerService : Task {
    @get:Internal
    val buildFinishedListenerService: Property<BuildFinishedListenerService>
}

abstract class BuildFinishedListenerService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val actionsOnClose = mutableListOf<() -> Unit>()
    private val keys = hashSetOf<String>()

    fun onClose(action: () -> Unit) {
        actionsOnClose.add(action)
    }

    fun onCloseOnceByKey(key: String, action: () -> Unit) {
        if (keys.add(key)) {
            onClose(action)
        }
    }

    override fun close() {
        for (action in actionsOnClose) {
            action()
        }
        actionsOnClose.clear()
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<BuildFinishedListenerService> = project.gradle.sharedServices
            .registerIfAbsent(
                // Use class loader hashcode in case there are multiple class loaders in the same build
                "build-finished-listener_${BuildFinishedListenerService::class.java.classLoader.hashCode()}",
                BuildFinishedListenerService::class.java
            ) {}.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesBuildFinishedListenerService::class.java.name) {
                    project.tasks.withType<UsesBuildFinishedListenerService>().configureEach { task ->
                        task.usesService(serviceProvider)
                    }
                }
            }

        fun getInstance(project: Project): BuildFinishedListenerService = registerIfAbsent(project).get()
    }
}