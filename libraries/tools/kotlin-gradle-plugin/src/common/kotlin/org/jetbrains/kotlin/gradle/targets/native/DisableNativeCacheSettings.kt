/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.Serializable
import java.net.URI

internal data class DisableNativeCacheSettings(
    val version: KotlinToolingVersion,
    val reason: String,
    val issueUrl: URI?,
) : Serializable

@OptIn(KotlinNativeCacheApi::class)
internal fun DisableCacheInKotlinVersion.toKotlinVersion() = KotlinToolingVersion(major, minor, patch, null)

/**
 * Checks whether this entry disables the cache for [nativeVersion].
 *
 * [DisableCacheInKotlinVersion] has constants only for the `X.Y.0` and `X.Y.20` releases, so a constant also covers
 * the bug-fix releases of its version: `2_4_0` covers 2.4.10, 2.4.11 and 2.4.12, and `2_4_20` covers 2.4.21 and 2.4.22.
 * A constant covers the patch numbers up to the next release, so `2_4_0` does not cover 2.4.20.
 */
internal fun DisableNativeCacheSettings.appliesTo(nativeVersion: KotlinToolingVersion): Boolean =
    version.major == nativeVersion.major &&
            version.minor == nativeVersion.minor &&
            version.patch / RELEASE_PATCH_STEP == nativeVersion.patch / RELEASE_PATCH_STEP

private const val RELEASE_PATCH_STEP = 20
