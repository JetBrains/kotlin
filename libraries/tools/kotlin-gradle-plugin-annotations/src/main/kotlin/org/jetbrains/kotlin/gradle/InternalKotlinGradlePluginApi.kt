/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@RequiresOptIn(
    message = "This API is not intended to be used outside of JetBrains / outside the Kotlin Gradle Plugin",
    level = RequiresOptIn.Level.ERROR
)
annotation class InternalKotlinGradlePluginApi
