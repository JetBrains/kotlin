/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

data class KotlinGradleNpmPackage(val simpleModuleName: String) : RequiredKotlinJsDependency {
    override fun createDependency(project: Project, scope: NpmDependency.Scope): Dependency =
        project.dependencies.create("org.jetbrains.kotlin:kotlin-$simpleModuleName")
}