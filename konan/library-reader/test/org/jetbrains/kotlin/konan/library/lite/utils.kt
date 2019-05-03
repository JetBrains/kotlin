/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import java.io.File

internal val testDataDir = File(System.getProperty("user.dir") ?: ".").canonicalFile.resolve("testData")

internal val konanHomeDir = testDataDir.resolve("kotlin-native-data-dir", "kotlin-native-PLATFORM-VERSION")

internal fun File.resolve(relative: String, next: String) = resolve(relative).resolve(next)

internal fun File.resolve(relative: String, next: String, another: String) = resolve(relative, next).resolve(another)
