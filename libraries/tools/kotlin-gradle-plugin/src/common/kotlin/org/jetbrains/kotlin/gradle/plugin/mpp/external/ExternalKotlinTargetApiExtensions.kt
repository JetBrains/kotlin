/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.*

@ExternalKotlinTargetApi
@Suppress("UnusedReceiverParameter", "DEPRECATION")
@Deprecated(
    "Accessing the project via the Kotlin extension is no longer supported. Scheduled for removal in Kotlin 2.4.",
    level = DeprecationLevel.ERROR
)
val KotlinTopLevelExtension.project: Project
    get() = throw UnsupportedOperationException()
