/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension

fun reproducibilityRootsMap(
        project: Project,
        nativeDependenciesExtension: NativeDependenciesExtension,
        jdkDir: File? = null,
): Map<File, String> = listOfNotNull(
        // This applies for both sources of the current project, and dependencies on other
        // projects inside the repo.
        project.isolated.rootProject.let {
            it.projectDirectory.asFile to it.name
        },
        // This is the common root for native dependencies: sysroots, llvm, ...
        nativeDependenciesExtension.nativeDependenciesRoot to "NATIVE_DEPS",
        // Not every user of `NativePlugin` uses JNI, but there's no harm to keep it for all.
        jdkDir?.let { it to "JDK" },
).toMap()

fun reproducibilityCompilerFlags(reproducibilityRootsMap: Map<File, String>): List<String> =
        reproducibilityRootsMap.map {
            "-ffile-prefix-map=${it.key}=${it.value}"
        }

fun reproducibilityCompilerFlags(
        project: Project,
        nativeDependenciesExtension: NativeDependenciesExtension,
        jdkDir: File? = null,
): List<String> = reproducibilityCompilerFlags(reproducibilityRootsMap(project, nativeDependenciesExtension, jdkDir))
