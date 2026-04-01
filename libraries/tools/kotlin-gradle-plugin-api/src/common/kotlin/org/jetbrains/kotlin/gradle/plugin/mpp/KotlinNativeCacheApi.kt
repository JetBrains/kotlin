/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

/**
 * Marks the Kotlin Native Cache DSL API as requiring explicit opt-in.
 *
 * Any declaration marked with `@KotlinNativeCacheApi` requires the caller to opt in
 * by adding `@OptIn(KotlinNativeCacheApi::class)` to the file or enclosing declaration.
 * This includes the `disableNativeCache` function and related types like `DisableCacheInKotlinVersion`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API requires opt-in. Use '@OptIn(KotlinNativeCacheApi::class)' on the file or declaration that calls this API."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@DslMarker
@MustBeDocumented
annotation class KotlinNativeCacheApi