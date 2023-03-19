/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.io.File

internal interface UsesCompositeNodeModulesCache : Task {
    @get:Internal
    val compositeNodeModules: Property<CompositeNodeModulesCache>
}

/**
 * Cache for storing already created [GradleNodeModule]s
 */
abstract class CompositeNodeModulesCache : AbstractNodeModulesCache() {
    override val type: String
        get() = "composite"

    override fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File? {
        val module = CompositeNodeModuleBuilder(file, parameters.cacheDir.get().asFile)
        return module.rebuild()
    }

    companion object {
        private val serviceClass = CompositeNodeModulesCache::class.java
        private val serviceName = serviceClass.name

        private fun registerIfAbsentImpl(
            project: Project,
            rootProjectDir: File?,
            cacheDir: File?
        ): Provider<CompositeNodeModulesCache> {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<CompositeNodeModulesCache>
            }

            val message = {
                "Build service Composite Node Modules should be already registered"
            }

            requireNotNull(rootProjectDir, message)
            requireNotNull(cacheDir, message)

            return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                it.parameters.rootProjectDir.set(rootProjectDir)
                it.parameters.cacheDir.set(cacheDir)
            }
        }

        fun registerIfAbsent(
            project: Project,
            rootProjectDir: File?,
            cacheDir: File?
        ) = registerIfAbsentImpl(project, rootProjectDir, cacheDir).also { serviceProvider ->
            SingleActionPerProject.run(project, UsesCompositeNodeModulesCache::class.java.name) {
                project.tasks.withType<UsesCompositeNodeModulesCache>().configureEach { task ->
                    task.usesService(serviceProvider)
                }
            }
        }
    }
}