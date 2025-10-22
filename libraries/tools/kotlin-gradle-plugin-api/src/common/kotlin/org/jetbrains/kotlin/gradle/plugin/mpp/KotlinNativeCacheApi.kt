/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

/**
 * This annotation marks the API for the Native Cache DSL as experimental and context-specific.
 *
 * Any API component marked with `@KotlinNativeCacheApi` is intended for use only within
 * the specific DSL scopes provided by the Kotlin Gradle Plugin, such as the `disableNativeCache` function.
 * Using these components outside of their intended scope is not supported and may lead to
 * unexpected behavior or compilation errors.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is intended for use only within the Kotlin Native Cache DSL."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@DslMarker
@MustBeDocumented
annotation class KotlinNativeCacheApi