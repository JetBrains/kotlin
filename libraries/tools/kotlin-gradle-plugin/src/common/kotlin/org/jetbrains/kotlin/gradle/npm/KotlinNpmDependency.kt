/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Represents a dependency declaration on an npm package.
 */
@ExperimentalKotlinGradlePluginApi
@Deprecated("This interface is unnecessary. $DEPRECATION_MSG")
interface KotlinNpmDependency {

    val name: String

    /**
     * npm version format in `package.json`
     */
    val version: String

    val scope: @Suppress("DEPRECATION") Scope

    /**
     * The `package.json` section an npm dependency belongs to.
     */
    @Deprecated("This enum is unnecessary. $DEPRECATION_MSG")
    enum class Scope {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER
    }
}

private const val DEPRECATION_MSG =
    "Npm dependencies should not be created manually, instead use the npm()/npmDev()/npmOptional()/npmPeer() DSL functions in Kotlin source sets. " +
            "Example: `kotlin { sourceSets { webMain { dependencies { npm(...) } } }`. " +
            "Scheduled for removal in Kotlin 2.5.20."
