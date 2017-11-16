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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.util.GradleVersion

private val gradleVersion = GradleVersion.current()

internal fun shouldEnableGradleCache(): Boolean =
    gradleVersion >= GradleVersion.version("4.3")

internal fun <T : Task> T.useBuildCacheIfSupported() {
    when {
        gradleVersion >= GradleVersion.version("3.4") ->
            outputs.cacheIf("Gradle version is at least 4.3") { shouldEnableGradleCache() }

        // There was no 'cacheIf' method accepting a reason string before 3.4:
        gradleVersion >= GradleVersion.version("3.0") -> outputs.cacheIf { false }

        // Before 3.0, there was no build cache at all, no need to restrict anything
        else -> Unit
    }
}