/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.utils.getValue
import java.io.File

internal open class KotlinCompileTaskData(
    val taskName: String,
    @field:Transient // cannot be serialized for Gradle Instant Execution, but actually is not needed when a task is deserialized
    val compilation: KotlinCompilation<*>,
    val destinationDir: Property<File>,
    val useModuleDetection: Property<Boolean>
) {
    private val project: Project
        get() = compilation.target.project

    private val taskBuildDirectory: File by project.provider {
        File(File(compilation.target.project.buildDir, KOTLIN_BUILD_DIR_NAME), taskName)
    }

    val buildHistoryFile: File by project.provider {
        File(taskBuildDirectory, "build-history.bin")
    }

    var javaOutputDir: File? = null

    companion object {
        fun get(project: Project, taskName: String): KotlinCompileTaskData = project.getTaskDataMap().getValue(taskName)

        fun getTaskDataContainer(project: Project): Iterable<KotlinCompileTaskData> =
            project.getTaskDataMap().values.toSet()

        fun register(
            taskName: String,
            compilation: KotlinCompilation<*>
        ): KotlinCompileTaskData {
            val project = compilation.target.project
            val container = project.getTaskDataMap()

            @Suppress("UnstableApiUsage")
            return KotlinCompileTaskData(
                taskName,
                compilation,
                project.objects.property(File::class.java).apply {
                    set(project.provider { error("destinationDir was not set for task ${project.path}$taskName") })
                },
                project.objects.property(Boolean::class.javaObjectType).apply { set(false) }
            ).also {
                container[taskName] = it
            }
        }

        private fun Project.getTaskDataMap(): MutableMap</* taskName: */ String, KotlinCompileTaskData> {
            val ext = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            if (!ext.has(EXT_NAME)) {
                ext.set(EXT_NAME, mutableMapOf<String, KotlinCompileTaskData>())
            }
            @Suppress("UNCHECKED_CAST")
            return ext.get(EXT_NAME) as MutableMap<String, KotlinCompileTaskData>
        }

        private const val EXT_NAME = "kotlin.incremental.taskdata"
    }
}