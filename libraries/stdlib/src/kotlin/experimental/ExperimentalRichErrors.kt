/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * Marks an API related to the experimental feature "rich errors" [KEEP-462](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0462-rich-errors.md).
 */
@RequiresOptIn(
    "This API is related to the experimental feature \"rich errors\" (see KEEP-462) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR,
)
@MustBeDocumented
public annotation class ExperimentalRichErrors
