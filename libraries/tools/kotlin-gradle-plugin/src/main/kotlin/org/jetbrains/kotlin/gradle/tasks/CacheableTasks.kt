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
import org.jetbrains.kotlin.gradle.utils.outputsCompatible

internal fun isBuildCacheSupported(): Boolean =
    gradleVersion >= GradleVersion.version("4.3")

internal fun isWorkerAPISupported(): Boolean =
    gradleVersion >= GradleVersion.version("4.3")

internal fun isBuildCacheEnabledForKotlin(): Boolean =
    isBuildCacheSupported() &&
            System.getProperty(KOTLIN_CACHING_ENABLED_PROPERTY)?.toBoolean() ?: true

internal fun <T : Task> T.cacheOnlyIfEnabledForKotlin() {
    // The `cacheIf` method may be missing if the Gradle version is too low:
    try {
        outputsCompatible.cacheIf { isBuildCacheEnabledForKotlin() }
    } catch (_: NoSuchMethodError) {
    }
}

private val gradleVersion = GradleVersion.current()

private const val KOTLIN_CACHING_ENABLED_PROPERTY = "kotlin.caching.enabled"
