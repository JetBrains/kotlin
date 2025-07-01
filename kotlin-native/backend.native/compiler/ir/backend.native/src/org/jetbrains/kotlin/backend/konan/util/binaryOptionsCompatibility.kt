/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.konan.target.SanitizerKind as SanitizerKindObsolete

internal fun SanitizerKind.toObsoleteKind(): SanitizerKindObsolete = when (this) {
    SanitizerKind.ADDRESS -> SanitizerKindObsolete.ADDRESS
    SanitizerKind.THREAD -> SanitizerKindObsolete.THREAD
}