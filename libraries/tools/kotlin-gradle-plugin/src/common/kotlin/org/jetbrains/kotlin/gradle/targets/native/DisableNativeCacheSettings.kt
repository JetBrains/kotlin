/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import java.io.Serializable
import java.net.URI

internal data class DisableNativeCacheSettings(
    val version: KotlinVersion,
    val reason: String,
    val issueUrl: URI?,
) : Serializable

@OptIn(KotlinNativeCacheApi::class)
internal fun DisableCacheInKotlinVersion.toKotlinVersion() = KotlinVersion(major, minor, patch)