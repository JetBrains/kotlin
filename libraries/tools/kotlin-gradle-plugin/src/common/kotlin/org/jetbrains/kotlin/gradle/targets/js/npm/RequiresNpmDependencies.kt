/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

interface RequiresNpmDependencies {
    @get:Internal
    val compilation: KotlinJsIrCompilation

    @get:Internal
    val requiredNpmDependencies: Set<RequiredKotlinJsDependency>

    /**
     * No longer required. Scheduled for removal in Kotlin 2.7.
     *
     * This function was initially introduced because some subtypes implemented [Task],
     * and [Task.getPath] was used in [org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements.byTask].
     * However, this is no longer required, so this function can be removed.
     */
    // TODO when you delete this getPath(), you must also delete RequiresNpmDependenciesTask.getPath()
    @Internal
    @Deprecated("No longer required. Scheduled for removal in Kotlin 2.7.")
    fun getPath(): String = "not-used"
}
