/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.util.GradleVersion
import java.io.File
import java.io.Serializable

const val minSupportedGradleVersion = "6.7.1"

internal fun checkGradleCompatibility(
    withComponent: String = "the Kotlin Gradle plugin",
    minSupportedVersion: GradleVersion = GradleVersion.version(minSupportedGradleVersion)
) {
    val currentVersion = GradleVersion.current()
    if (currentVersion < minSupportedVersion) {
        throw GradleException(
            "The current Gradle version ${currentVersion.version} is not compatible with $withComponent. " +
                    "Please use Gradle ${minSupportedVersion.version} or newer, or the previous version of the Kotlin plugin."
        )
    }
}

internal val AbstractArchiveTask.archivePathCompatible: File
    get() = archiveFile.get().asFile

internal class ArchiveOperationsCompat(@Transient private val project: Project) : Serializable {
    private val archiveOperations: Any? = try {
        (project as ProjectInternal).services.get(ArchiveOperations::class.java)
    } catch (e: NoClassDefFoundError) {
        // Gradle version < 6.6
        null
    }

    fun zipTree(obj: Any): FileTree {
        return when (archiveOperations) {
            is ArchiveOperations -> archiveOperations.zipTree(obj)
            else -> project.zipTree(obj)
        }
    }

    fun tarTree(obj: Any): FileTree {
        return when (archiveOperations) {
            is ArchiveOperations -> archiveOperations.tarTree(obj)
            else -> project.tarTree(obj)
        }
    }
}

internal class FileSystemOperationsCompat(@Transient private val project: Project) : Serializable {
    private val fileSystemOperations: Any? = try {
        (project as ProjectInternal).services.get(FileSystemOperations::class.java)
    } catch (e: NoClassDefFoundError) {
        // Gradle version < 6.0
        null
    }

    fun copy(action: (CopySpec) -> Unit): WorkResult? {
        return when (fileSystemOperations) {
            is FileSystemOperations -> fileSystemOperations.copy(action)
            else -> project.copy(action)
        }
    }
}

// Gradle dropped out getOwnerBuildOperationId. Workaround to build correct plugin for Gradle < 6.8
// See https://github.com/gradle/gradle/commit/0296f4441ae69ad608cfef6a90fef3fdf314fa2c
internal interface LegacyTestDescriptorInternal : TestDescriptor {
    override fun getParent(): TestDescriptorInternal?

    fun getId(): Any?

    fun getOwnerBuildOperationId(): Any?

    fun getClassDisplayName(): String?
}
