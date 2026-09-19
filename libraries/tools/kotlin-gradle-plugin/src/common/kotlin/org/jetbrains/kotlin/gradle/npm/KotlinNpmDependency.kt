/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

internal interface KotlinNpmDependency {

    val name: String

    /**
     * npm version format in `package.json`
     */
    val version: String

    val scope: Scope

    /**
     * The `package.json` section an npm dependency belongs to.
     */
    enum class Scope {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER
    }
}
