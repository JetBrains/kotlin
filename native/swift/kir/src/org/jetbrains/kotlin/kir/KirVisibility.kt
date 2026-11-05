/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir

enum class KirVisibility : Comparable<KirVisibility> {
    PRIVATE,
    PROTECTED,
    INTERNAL,
    PUBLIC,
}

val KirVisibility.value: String
    get() = when (this) {
        KirVisibility.PRIVATE -> "private"
        KirVisibility.PROTECTED -> "protected"
        KirVisibility.INTERNAL -> "internal"
        KirVisibility.PUBLIC -> "public"
    }
