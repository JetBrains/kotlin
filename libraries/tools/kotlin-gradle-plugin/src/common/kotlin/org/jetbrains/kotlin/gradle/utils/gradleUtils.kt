/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

val Gradle.projectCacheDir
    get() = startParameter.projectCacheDir ?: this.rootProject.projectDir.resolve(".gradle")

internal val Project.compositeBuildRootProject: Project get() = generateSequence(project.gradle) { it.parent }.last().rootProject
