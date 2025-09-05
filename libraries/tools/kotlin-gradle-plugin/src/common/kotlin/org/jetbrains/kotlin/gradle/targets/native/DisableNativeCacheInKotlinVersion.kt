/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.Serializable
import java.net.URI

/**
 * Represents a specific Kotlin version for which native caching can be disabled via the DSL.
 *
 * The constructor is private to ensure that only predefined, valid instances
 * provided in the companion object can be used.
 *
 * This class is marked with `@KotlinNativeCacheApi` and should only be used within the
 * context of the `disableNativeCache` function.
 */
@KotlinNativeCacheApi
class DisableNativeCacheInKotlinVersion private constructor(internal val version: KotlinToolingVersion) : Serializable {

    override fun toString(): String = version.toString()

    @Suppress("unused")
    companion object {
        /** Disables native cache for Kotlin version 2.3.0 */
        val `2_3_0` = DisableNativeCacheInKotlinVersion(KotlinToolingVersion(2, 3, 0, null))

        /** Disables native cache for Kotlin version 2.4.0 */
        val `2_4_0` = DisableNativeCacheInKotlinVersion(KotlinToolingVersion(2, 4, 0, null))

        // As new versions with cache-related bugs are found,
        // the Kotlin team would add new properties here.
    }
}

@KotlinNativeCacheApi
internal data class DisableNativeCacheSettings(
    val version: DisableNativeCacheInKotlinVersion,
    val reason: String,
    val issueUrl: URI?,
) : Serializable