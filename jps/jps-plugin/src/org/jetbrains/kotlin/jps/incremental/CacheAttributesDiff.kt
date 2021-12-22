/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

/**
 * Diff between actual and expected cache attributes.
 * [status] are calculated based on this diff (see [CacheStatus]).
 * Based on that [status] system may perform required actions (i.e. rebuild something, clearing caches, etc...).
 *
 * [CacheAttributesDiff] can be used to cache current attribute values and as facade for version operations.
 */
data class CacheAttributesDiff<Attrs: Any>(
    val manager: CacheAttributesManager<Attrs>,
    val actual: Attrs?,
    val expected: Attrs?
) {
    val status: CacheStatus
        get() =
            if (expected != null) {
                if (actual != null && manager.isCompatible(actual, expected)) CacheStatus.VALID
                else CacheStatus.INVALID
            } else {
                if (actual != null) CacheStatus.SHOULD_BE_CLEARED
                else CacheStatus.CLEARED
            }

    override fun toString(): String {
        return "$status: actual=$actual -> expected=$expected"
    }
}