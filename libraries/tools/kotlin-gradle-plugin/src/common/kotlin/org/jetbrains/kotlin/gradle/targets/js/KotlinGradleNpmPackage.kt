/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.io.Serializable

data class KotlinGradleNpmPackage(val simpleModuleName: String) : Serializable {
    fun createDependency(project: Project): Dependency =
        project.dependencies.create("org.jetbrains.kotlin:kotlin-$simpleModuleName")
}