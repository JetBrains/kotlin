/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.jetbrains.kotlin.gradle.targets.js.npm.*
import java.io.File
import java.io.Serializable

/**
 * Resolved [NpmProject]
 */
class PreparedKotlinCompilationNpmResolution(
    val npmProjectDir: File,
    val externalGradleDependencies: Collection<GradleNodeModule>,
    val externalNpmDependencies: Collection<NpmDependencyDeclaration>,
) : Serializable