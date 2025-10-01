/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.Serializable

/**
 * Represents a specific Kotlin version for which native caching can be disabled via the DSL.
 */
@KotlinNativeCacheApi
class DisableNativeCacheInKotlinVersion/* internal constructor*/(
    val version: KotlinToolingVersion
) : Serializable {

    override fun toString(): String = version.toString()

    @Suppress("unused")
    companion object {
        /** Disables native cache for Kotlin version 2.3.0 */
        val `2_3_0` = DisableNativeCacheInKotlinVersion(KotlinToolingVersion(2, 3, 0, null))
        val snapshot = DisableNativeCacheInKotlinVersion(KotlinToolingVersion(2, 3, 255, "snapshot"))
    }
}