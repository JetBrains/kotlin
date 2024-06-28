/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModule
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import java.io.Serializable

/**
 * Resolved [NpmProject]
 */
class PreparedKotlinCompilationNpmResolution(
    val npmProjectDir: Provider<Directory>,
    val externalGradleDependencies: Collection<GradleNodeModule>,
    val externalNpmDependencies: Collection<NpmDependencyDeclaration>,
) : Serializable