/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

abstract class KonanCliRunnerIsolatedClassLoadersService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    val isolatedClassLoaders = ConcurrentHashMap<Any, URLClassLoader>()

    override fun close() {
        isolatedClassLoaders.clear()
    }

    companion object {
        fun registerIfAbsent(project: Project) = project.gradle.sharedServices.registerIfAbsent("KonanCliRunnerIsolatedClassLoadersService", KonanCliRunnerIsolatedClassLoadersService::class.java) {}

        fun attachingToTask(task: Task): KonanCliRunnerIsolatedClassLoadersService {
            val service = registerIfAbsent(task.project)
            task.usesService(service)
            return service.get()
        }
    }
}