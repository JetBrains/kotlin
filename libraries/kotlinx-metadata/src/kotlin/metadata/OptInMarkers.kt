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
 * Marks an API related to the experimental feature "annotations in metadata", enabled by the compiler flag `-Xannotations-in-metadata`
 * (https://youtrack.jetbrains.com/issue/KT-57919).
 *
 * kotlin-metadata-jvm clients are encouraged to update the code that reads and writes Kotlin metadata with the support of new annotations,
 * before annotations in metadata become enabled by default (https://youtrack.jetbrains.com/issue/KT-75736).
 *
 * Note that we have one-version forward compatibility policy on Kotlin/JVM, so kotlin-metadata-jvm 2.2 can read and write metadata of
 * version 2.3. In case annotations in metadata are enabled by default in 2.3, not handling them via kotlin-metadata-jvm of version 2.2
 * will likely produce incorrect results on class files compiled by Kotlin 2.3. Clients can use the Kotlin compiler flag
 * `-Xannotations-in-metadata` to check that annotations in metadata are read/written correctly.
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"annotations in metadata\" (see KT-57919).",
    RequiresOptIn.Level.WARNING,
)
@MustBeDocumented
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
