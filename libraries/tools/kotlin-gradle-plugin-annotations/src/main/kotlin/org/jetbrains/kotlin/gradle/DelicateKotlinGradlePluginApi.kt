/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@RequiresOptIn(
    message = "This Kotlin Gradle Plugin API is considered 'low level' or 'easy to misuse'. In most cases another 'high level API' " +
            "is the better solution.",
    level = RequiresOptIn.Level.WARNING
)
annotation class DelicateKotlinGradlePluginApi