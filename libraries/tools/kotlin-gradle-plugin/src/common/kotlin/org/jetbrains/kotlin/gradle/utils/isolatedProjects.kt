/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.util.GradleVersion

/** Get the root project name (in an isolated-project-friendly way, if possible). */
internal fun Project.rootProjectName(): String =
    if (isIsolatedProjectAvailable) {
        isolated.rootProject.name
    } else {
        rootProject.name
    }

/** Check if this [Project] is the root project (in an isolated-project-friendly way, if possible). */
internal fun Project.isRootProject(): Boolean =
    if (isIsolatedProjectAvailable) {
        isolated == isolated.rootProject
    } else {
        this == rootProject
    }

/** Check whether [Project.getIsolated] is available. */
private val isIsolatedProjectAvailable: Boolean
    get() = GradleVersion.current() >= GradleVersion.version("8.8")
