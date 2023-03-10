/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

enum class SanitizerKind {
    ADDRESS,
    THREAD,
}

/**
 * Suffix for [KonanTarget] name.
 *
 * In string interpolation use
 * ```
 * "… ${target}${sanitizer.targetSuffix} …"
 * ```
 */
val SanitizerKind?.targetSuffix: String
    get() = when (this) {
        null -> ""
        SanitizerKind.THREAD -> "_tsan"
        SanitizerKind.ADDRESS -> "_asan"
    }
