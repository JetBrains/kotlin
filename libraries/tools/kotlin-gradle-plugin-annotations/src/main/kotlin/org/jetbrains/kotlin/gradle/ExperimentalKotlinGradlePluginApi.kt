/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@RequiresOptIn(
    message = "Experimental API in the Kotlin Gradle Plugin: No stability guarantees can be provided. " +
            "The API could get removed in the future without replacement",
    level = RequiresOptIn.Level.WARNING
)
annotation class ExperimentalKotlinGradlePluginApi
