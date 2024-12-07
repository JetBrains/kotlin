/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@RequiresOptIn(
    message = "This API is not intended to be used outside of the Kotlin Gradle Plugin and Compose Multiplatform Plugin integration",
    level = RequiresOptIn.Level.ERROR
)
annotation class ComposeKotlinGradlePluginApi
