/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.js

import kotlinx.serialization.Serializable

/**
 * A minimal representation of a `package.json` file.
 */
@Serializable
internal data class PackageJson(
    val name: String,
    val version: String? = null,
    val dependencies: Map<String, String> = emptyMap(),
    val devDependencies: Map<String, String> = emptyMap(),
    val peerDependencies: Map<String, String> = emptyMap(),
    val optionalDependencies: Map<String, String> = emptyMap(),
    val bundledDependencies: List<String> = emptyList(),
)
