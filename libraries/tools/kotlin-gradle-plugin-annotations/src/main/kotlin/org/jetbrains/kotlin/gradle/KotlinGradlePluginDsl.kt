/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

/**
 * Marker for public Kotlin Gradle Plugin APIs.
 *
 * This annotation has two contracts:
 * 1: Declarations annotated by this annotation will be binary compatibility validated
 * 2: Declarations annotated by this inherit the [DslMarker] semantics
 */
@DslMarker
annotation class KotlinGradlePluginDsl