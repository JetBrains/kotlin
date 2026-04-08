/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

enum class HotReloadSplitMode {
    GUEST,
    GUEST_IC,
    HOST;

    companion object {
        @JvmStatic
        fun fromString(value: String): HotReloadSplitMode? = when (value.lowercase()) {
            "guest" -> GUEST
            "guest-ic", "guest_ic" -> GUEST_IC
            "host" -> HOST
            else -> null
        }
    }
}