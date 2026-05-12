/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata

internal const val CtxReceiversDeprecated =
    "Context receivers feature is superseded with context parameters. Please use context parameters API instead. See documentation for details."

/**
 * Marks an API related to the Kotlin's [context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md) experimental feature.
 * This feature is superseded by context parameters.
 * Replace the '-Xcontext-receivers' compiler argument with '-Xcontext-parameters' and migrate to the new syntax.
 *
 * Marked API reflects metadata written by this feature, and can be changed or removed as development continues.
 * Therefore, it does not provide any compatibility guarantees.
 */
@RequiresOptIn(
    "The API is related to the experimental feature \"context receivers\" (see KEEP-259) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR
)
@MustBeDocumented
public annotation class ExperimentalContextReceivers

/**
 * This annotation marked API related to the feature "annotations in metadata" (https://youtrack.jetbrains.com/issue/KT-57919),
 * which was experimental before Kotlin 2.4.0.
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"annotations in metadata\" (see KT-57919).",
    RequiresOptIn.Level.WARNING,
)
@MustBeDocumented
@Deprecated("Annotations in metadata are enabled by default since Kotlin 2.4. This annotation has no effect.")
public annotation class ExperimentalAnnotationsInMetadata

/**
 * Marks an API related to the experimental feature "unused return value checker" [KT-12719](https://youtrack.jetbrains.com/issue/KT-12719),
 * enabled by the compiler flag `-Xreturn-value-checker`.
 *
 * This feature uses kotlin metadata to store information whether the return value of a function should be checked for usage.
 * See [KmFunction.returnValueStatus] and [ReturnValueStatus] for details.
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"unused return value checker\" (see KT-12719).",
    RequiresOptIn.Level.WARNING,
)
@MustBeDocumented
public annotation class ExperimentalMustUseStatus

/**
 * Marks an API related to the experimental feature "companion blocks and extensions" [KEEP-449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md).
 *
 * Functions and properties introduced in companion blocks and extensions are compiled as static on JVM,
 * and have [KmFunction.isStatic] and [KmProperty.isStatic] attributes set to `true`.
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"companion blocks and extensions\" (see KEEP-449) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR,
)
@MustBeDocumented
public annotation class ExperimentalCompanionBlocksAndExtensions
