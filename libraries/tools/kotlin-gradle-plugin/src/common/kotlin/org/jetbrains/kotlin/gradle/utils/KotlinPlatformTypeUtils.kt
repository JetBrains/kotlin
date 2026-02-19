/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType


internal val KotlinPlatformType.prettyName: String
    get() = when (this) {
        KotlinPlatformType.common -> "Kotlin/Common"
        KotlinPlatformType.jvm -> "Kotlin/JVM"
        KotlinPlatformType.js -> "Kotlin/JS"
        KotlinPlatformType.androidJvm -> "Kotlin/Android"
        KotlinPlatformType.native -> "Kotlin/Native"
        KotlinPlatformType.wasm -> "Kotlin/Wasm"
    }
