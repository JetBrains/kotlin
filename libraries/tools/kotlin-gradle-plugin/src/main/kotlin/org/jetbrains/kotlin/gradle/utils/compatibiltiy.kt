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
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskInputs
import org.gradle.api.tasks.TaskOutputs
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GradleVersion
import java.io.File

const val minSupportedGradleVersion = "6.1"

internal val Task.inputsCompatible: TaskInputs get() = inputs

internal val Task.outputsCompatible: TaskOutputs get() = outputs

private val propertyMethod by lazy {
    TaskInputs::class.java.methods.first {
        it.name == "property" && it.parameterTypes.contentEquals(arrayOf(String::class.java, Any::class.java))
    }
}

internal fun TaskInputs.propertyCompatible(name: String, value: Any) {
    propertyMethod(this, name, value)
}

private val inputsDirMethod by lazy {
    TaskInputs::class.java.methods.first {
        it.name == "dir" && it.parameterTypes.contentEquals(arrayOf(Any::class.java))
    }
}

internal fun TaskInputs.dirCompatible(dirPath: Any) {
    inputsDirMethod(this, dirPath)
}

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

internal class ArchiveOperationsCompat(@Transient private val project: Project) {
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

internal class FileSystemOperationsCompat(@Transient private val project: Project) {
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