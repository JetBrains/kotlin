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
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.util.GradleVersion
import java.io.File

const val minSupportedGradleVersion = "6.8.3"

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

// Gradle dropped out getOwnerBuildOperationId. Workaround to build correct plugin for Gradle < 6.8
// See https://github.com/gradle/gradle/commit/0296f4441ae69ad608cfef6a90fef3fdf314fa2c
internal interface LegacyTestDescriptorInternal : TestDescriptor {
    override fun getParent(): TestDescriptorInternal?

    fun getId(): Any?

    fun getOwnerBuildOperationId(): Any?

    fun getClassDisplayName(): String?
}

/**
 * According to [Gradle 7.3 release notes](https://docs.gradle.org/7.3/release-notes.html#allow-plugin-authors-to-declare-tasks-as-untracked)
 * [Task.doNotTrackState] is a better replacement for `Task.outputs.upToDateWhen { false }`
 */
internal fun Task.doNotTrackStateCompat(because: String) {
    if (GradleVersion.current() < GradleVersion.version("7.3")) {
        logger.info("Not UP-TO-DATE because $because")
        outputs.upToDateWhen { false }
    } else {
        doNotTrackState(because)
    }
}

/**
 * According to [Gradle 7.6 release notes](https://docs.gradle.org/7.6/release-notes.html#introduced-ability-to-explain-why-a-task-was-skipped-with-a-message)
 * [Task.onlyIf] has reason message`
 */
internal fun Task.onlyIfCompat(onlyIfReason: String, onlyIfSpec: Spec<in Task>) {
    if (GradleVersion.current() < GradleVersion.version("7.6")) {
        onlyIf(onlyIfSpec)
    } else {
        onlyIf(onlyIfReason, onlyIfSpec)
    }
}

/**
 * [ArtifactCollection.getResolvedArtifacts] is available after 7.4 (inclusive)
 */
internal fun ArtifactCollection.getResolvedArtifactsCompat(project: Project): Provider<Set<ResolvedArtifactResult>> =
    if (GradleVersion.current() >= GradleVersion.version("7.4")) {
        resolvedArtifacts
    } else {
        project.provider { artifacts }
    }

/**
 * ValueSources with injected ExecOperations are supported with Configuration Cache in Gradle 7.5+
 * https://docs.gradle.org/7.5/release-notes.html#running-external-processes-at-configuration-time
 */
internal fun <T> Project.valueSourceWithExecProviderCompat(
    clazz: Class<out ValueSource<T, ValueSourceParameters.None>>
): Provider<T> {
    return if (GradleVersion.current() < GradleVersion.version("7.5")) {
        val vs = project.objects.newInstance(clazz)
        project.provider { vs.obtain() }
    } else {
        providers.of(clazz) { }
    }
}