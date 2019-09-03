/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModule
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson

/**
 * Resolved [NpmProject]
 */
class KotlinCompilationNpmResolution(
    val project: Project,
    val npmProject: NpmProject,
    val internalDependencies: Collection<KotlinCompilationNpmResolution>,
    val externalGradleDependencies: Collection<GradleNodeModule>,
    val externalNpmDependencies: Collection<NpmDependency>,
    val packageJson: PackageJson
)