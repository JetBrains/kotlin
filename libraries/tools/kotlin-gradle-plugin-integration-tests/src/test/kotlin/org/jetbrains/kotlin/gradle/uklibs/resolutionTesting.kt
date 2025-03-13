/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.project.ProjectStateRegistry
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import java.io.File
import kotlin.io.path.pathString

/**
 * buildScriptReturn injections execute in the FlowAction build finish callback. Unfortunately Gradle prohibits resolving configurations
 * there, but we can use this workaround to suppress the check
 */
fun <T> Project.ignoreAccessViolations(code: () -> (T)) = (project.gradle as GradleInternal).services.get(
    ProjectStateRegistry::class.java
).allowUncontrolledAccessToAnyProject { code() }
