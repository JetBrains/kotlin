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
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GradleVersion
import java.io.File

internal fun checkGradleCompatibility(minSupportedVersion: GradleVersion = GradleVersion.version("5.4")) {
    val currentVersion = GradleVersion.current()
    if (currentVersion < minSupportedVersion) {
        throw GradleException(
            "Current version of Gradle $currentVersion is not compatible with Kotlin plugin. " +
                    "Please use Gradle $minSupportedVersion or newer or previous version of Kotlin plugin."
        )
    }
}

internal val AbstractArchiveTask.archivePathCompatible: File
    get() = archiveFile.get().asFile