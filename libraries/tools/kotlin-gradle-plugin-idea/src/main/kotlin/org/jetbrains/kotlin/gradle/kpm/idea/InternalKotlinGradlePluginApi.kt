/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API can only be used by the Kotlin Gradle Plugin and is not kept stable for access inside the IDE process"
)
annotation class InternalKotlinGradlePluginApi
