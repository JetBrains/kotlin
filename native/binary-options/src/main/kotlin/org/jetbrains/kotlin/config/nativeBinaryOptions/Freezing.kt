/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * Gradually control what parts of freezing are enabled.
 * Not intended to be used with legacy MM where freezing is a must.
 */
enum class Freezing {
    /**
     * Enable freezing in `Any.freeze()` as well as for @Frozen types and @SharedImmutable globals.
     */
    Full,

    /**
     * Enable freezing only in explicit calls to `Any.freeze()`.
     */
    ExplicitOnly,

    /**
     * No freezing at all.
     */
    Disabled;

}