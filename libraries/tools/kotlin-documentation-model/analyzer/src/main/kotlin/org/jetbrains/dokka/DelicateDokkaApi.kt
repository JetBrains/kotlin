/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

/**
 * Marks declarations in the Dokka that are **delicate** &mdash;
 * they have limited use-case and shall be used with care in general code.
 * Any use of a delicate declaration has to be carefully reviewed to make sure it is
 * properly used and does not create problems like memory and resource leaks.
 * Carefully read documentation of any declaration marked as `DelicateDokkaApi`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate API and its use requires care." +
            " Make sure you fully read and understand documentation of the declaration that is marked as a delicate API."
)
@Retention(AnnotationRetention.BINARY)
public annotation class DelicateDokkaApi
