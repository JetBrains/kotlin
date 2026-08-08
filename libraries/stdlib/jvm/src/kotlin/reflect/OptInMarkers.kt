/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Marks an API related to the experimental feature "companion extensions" [KEEP-449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md).
 *
 * Functions and properties introduced in companion extensions have
 * [KCallable.companionExtensionClass] set to the corresponding [KClass].
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"companion extensions\" (see KEEP-449) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR,
)
@MustBeDocumented
public annotation class ExperimentalCompanionExtensions
