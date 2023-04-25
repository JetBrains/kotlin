/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.report.reportingSettings
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.io.File
import javax.inject.Inject

internal interface UsesGradleNodeModulesCache : Task {
    @get:Internal
    val gradleNodeModules: Property<GradleNodeModulesCache>
}

/**
 * Cache for storing already created [GradleNodeModule]s
 */
abstract class GradleNodeModulesCache : AbstractNodeModulesCache() {

    @get:Inject
    abstract val fs: FileSystemOperations

    override val type: String
        get() = "gradle"

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    override fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File? {
        val module = GradleNodeModuleBuilder(fs, archiveOperations, name, version, listOf(file), parameters.cacheDir.get().asFile)
        module.visitArtifacts()
        return module.rebuild()
    }

    companion object {
        private val serviceClass = GradleNodeModulesCache::class.java
        private val serviceName = serviceClass.name

        private fun registerIfAbsentImpl(
            project: Project,
            rootProjectDir: File?,
            cacheDir: File?
        ): Provider<GradleNodeModulesCache> {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<GradleNodeModulesCache>
            }

            val message = {
                "Build service Gradle Node Modules should be already registered"
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
            SingleActionPerProject.run(project, UsesGradleNodeModulesCache::class.java.name) {
                project.tasks.withType<UsesGradleNodeModulesCache>().configureEach { task ->
                    task.usesService(serviceProvider)
                }
            }
        }
    }
}