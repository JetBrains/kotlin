/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class UklibManifest(
    val fragments: List<UklibManifestFragment>,
    val manifestVersion: String,
)

@Serializable
internal data class UklibManifestFragment(
    val identifier: String,
    val targets: List<String>,
    val files: List<String>? = null,
)
