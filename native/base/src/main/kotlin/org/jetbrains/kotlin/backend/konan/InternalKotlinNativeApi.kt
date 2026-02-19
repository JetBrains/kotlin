/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

/**
 * This annotation is used to mark APIs as 'internal' to Kotlin/Native across modules within kotlin.git.
 * Note: Any API with this annotation can be changed and potentially removed at any time.
 * It is not safe to depend on any internal Kotlin/Native API outside kotlin.git/kotlin-native
 */
@RequiresOptIn(
    "This API is internal to the Kotlin/Native compiler and cannot be used outside of kotlin.git",
    level = RequiresOptIn.Level.ERROR
)
annotation class InternalKotlinNativeApi
