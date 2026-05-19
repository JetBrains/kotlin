/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.tasks.Internal

/**
 * A Gradle [org.gradle.api.Task] that uses npm dependencies.
 *
 * @see RequiresNpmDependencies
 */
interface RequiresNpmDependenciesTask : RequiresNpmDependencies, Task {
    /**
     * Temporary replacement, because [RequiresNpmDependencies.getPath] is deprecated.
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * This function cannot be deprecated because [Task.getPath] is not deprecated.
     * https://youtrack.jetbrains.com/issue/KTLC-156/Do-not-propagate-method-deprecation-through-overrides.
     *
     * @see Task.getPath
     */
    @Internal
    override fun getPath(): String
}
