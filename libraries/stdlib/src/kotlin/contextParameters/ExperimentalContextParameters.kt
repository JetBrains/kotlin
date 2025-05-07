/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Marks an API related to the Kotlin's [context parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md)
 * experimental feature.
 *
 * Marked API depends on the design of this feature, and can be changed or removed as development continues.
 * Therefore, it does not provide any compatibility guarantees.
 */
@RequiresOptIn(
    "The API is related to the experimental feature \"context parameters\" (see KEEP-367) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.2")
public annotation class ExperimentalContextParameters
