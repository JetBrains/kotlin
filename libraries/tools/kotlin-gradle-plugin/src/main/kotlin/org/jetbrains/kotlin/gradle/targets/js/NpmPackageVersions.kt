/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "2.2.0")
val karmaPhantomjsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "*")
// karma-firefox-launcher
// karma-opera-launcher
// karma-ie-launcher
// karma-safari-launcher

data class NpmPackageVersion(val name: String, val version: String) {
    fun npm(project: Project) = NpmDependency(project, null, name, version)
}
