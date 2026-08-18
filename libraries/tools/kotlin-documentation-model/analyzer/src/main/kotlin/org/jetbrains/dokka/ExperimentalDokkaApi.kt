/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

/**
 * Marks declarations that are still **experimental** in Dokka's API.
 *
 * Experimental declarations may be changed in the future releases in ways that break compatibility,
 * or may be removed entirely. Using experimental declarations should be done with extra care and
 * acknowledgment of potential future breaking changes.
 *
 * Carefully read documentation of any declaration marked as `ExperimentalDokkaApi`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and may change in the future." +
            " Make sure you fully read and understand documentation of the declaration that is marked as experimental."
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalDokkaApi