/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

/**
 * Marks an API related to the Kotlin's [context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md) experimental feature.
 *
 * Marked API reflects metadata written by this feature, and can be changed or removed as development continues.
 * Therefore, it does not provide any compatibility guarantees.
 */
@RequiresOptIn(
    "The API is related to the experimental feature \"context receivers\" (see KEEP-259) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR
)
@MustBeDocumented
annotation class ExperimentalContextReceivers
