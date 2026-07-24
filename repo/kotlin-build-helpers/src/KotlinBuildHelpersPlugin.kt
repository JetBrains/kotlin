/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Plugin
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Just a marker plugin to make classes like [KotlinBuildProperties] available on the buildscript's classpath
 */
class KotlinBuildHelpersPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        // no-op
    }
}

/**
 * Syntactic sugar for adding a dependency to `kotlin-build-helpers`:
 * ```kotlin
 * dependencies {
 *     implementation(kotlinBuildHelpers())
 * }
 * ```
 */
fun DependencyHandler.kotlinBuildHelpers(): String =
    "org.jetbrains.kotlin:kotlin-build-helpers"
