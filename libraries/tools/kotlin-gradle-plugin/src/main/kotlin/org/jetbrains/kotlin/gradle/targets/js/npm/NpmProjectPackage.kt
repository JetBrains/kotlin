/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project

/**
 * [NpmProject] full package info
 */
class NpmProjectPackage(
    val project: Project,
    val npmProject: NpmProject,
    val npmDependencies: Collection<NpmDependency>,
    val gradleDependencies: NpmGradleDependencies,
    val packageJson: PackageJson,
    val hasNodeModulesDependentTasks: Boolean
)