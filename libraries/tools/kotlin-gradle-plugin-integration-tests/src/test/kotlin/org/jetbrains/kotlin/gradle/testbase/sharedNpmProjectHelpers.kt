/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Applies [KotlinSharedNpmProjectPlugin] to this project.
 *
 * The plugin assembles the shared npm root project of the build out of the `package.json` files
 * of all the compilations of the build, which is how npm dependencies are resolved
 * when Gradle Isolated Projects is enabled.
 *
 * A Kotlin Multiplatform build is not required to apply the plugin to its root project,
 * so the tests that are verified with Gradle Isolated Projects enabled have to apply it explicitly.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun GradleProject.applyKotlinSharedNpmProjectPlugin() {
    // The plugin is applied through a build script injection, which requires a build script to exist,
    // while a root project of a test build is allowed to have none.
    if (!buildGradle.exists() && !buildGradleKts.exists()) {
        buildGradleKts.createFile()
    }

    buildScriptInjection {
        try {
            project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
        } catch (_: NoClassDefFoundError) {
            // The Kotlin Gradle Plugin is not necessarily on the class path of the root project of a test build,
            // for example when only its subprojects apply it. Such a build has no shared npm project,
            // and therefore is not expected to be verified with Gradle Isolated Projects enabled.
        }
    }

    // The injection is appended to the build script without a trailing line separator,
    // so anything a test appends afterwards would end up on the same line.
    val buildScript = if (buildGradleKts.exists()) buildGradleKts else buildGradle
    if (!buildScript.readText().endsWith("\n")) {
        buildScript.appendText("\n")
    }
}
