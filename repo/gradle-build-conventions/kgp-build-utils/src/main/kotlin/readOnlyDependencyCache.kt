/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.Test

/**
 * Lets Gradle builds started by [task] reuse the dependencies already downloaded by this build,
 * by setting `GRADLE_RO_DEP_CACHE` to this build's dependency cache.
 *
 * Does nothing if the cache doesn't exist, or if `GRADLE_RO_DEP_CACHE` is already set.
 *
 * See https://docs.gradle.org/current/userguide/dependency_caching.html#sec:shared-readonly-cache
 */
fun useHostDependencyCacheAsReadOnly(task: Test) {
    val hostDependencyCache = task.project.gradle.gradleUserHomeDir.resolve("caches")
    val readOnlyCacheAlreadySet = task.project.providers.environmentVariable("GRADLE_RO_DEP_CACHE").isPresent
    if (hostDependencyCache.exists() && !readOnlyCacheAlreadySet) {
        task.environment("GRADLE_RO_DEP_CACHE", hostDependencyCache.invariantSeparatorsPath)
    }
}
