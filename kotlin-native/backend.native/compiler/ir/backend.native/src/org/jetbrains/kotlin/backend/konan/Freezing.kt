/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

/**
 * Gradually control what parts of freezing are enabled.
 * Not intended to be used with legacy MM where freezing is a must.
 *
 * If [enableFreezeAtRuntime] is false then `Any.freeze()` and `checkIfFrozen(ref: Any?)` are no-op.
 * [freezeImplicit] enabled freezing for @Frozen types and @SharedImmutable globals (i.e. implicit calls to `Any.freeze()`).
 * If [enableFreezeChecks] is false, than `Any.isFrozen() will always return false, and no [InvalidMutabilityException] can be thrown
 */
enum class Freezing(val enableFreezeAtRuntime: Boolean, val freezeImplicit: Boolean, val enableFreezeChecks: Boolean) {
    /**
     * Enable freezing in `Any.freeze()` as well as for @Frozen types and @SharedImmutable globals.
     */
    Full(true, true, true),

    /**
     * Enable freezing only in explicit calls to `Any.freeze()`.
     */
    ExplicitOnly(true, false, true),

    /**
     * No freezing at all.
     */
    Disabled(false, false, false);

}