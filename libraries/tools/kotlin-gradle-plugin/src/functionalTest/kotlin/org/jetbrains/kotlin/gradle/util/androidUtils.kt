/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.junit.Assume
import java.io.File

val isAndroidSdkAvailable: Boolean = System.getenv("ANDROID_SDK_ROOT")?.let { root -> File(root).exists() } ?: false

fun assumeAndroidSdkAvailable() {
    Assume.assumeTrue("Missing android sdk", isAndroidSdkAvailable)
}
