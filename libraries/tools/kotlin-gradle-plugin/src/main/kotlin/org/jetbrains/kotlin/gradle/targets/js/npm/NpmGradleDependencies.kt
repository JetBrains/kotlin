/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project

/**
 * Gradle dependencies for [NpmProject].
 */
class NpmGradleDependencies {
    val internalModules = mutableSetOf<Project>()
    val externalModules = mutableSetOf<GradleNodeModule>()

    fun addAll(other: NpmGradleDependencies) {
        internalModules.addAll(other.internalModules)
        externalModules.addAll(other.externalModules)
    }
}