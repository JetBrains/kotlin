/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import java.io.File

const val KONAN_STDLIB_NAME = "stdlib"

const val KONAN_DISTRIBUTION_KLIB_DIR = "klib"
const val KONAN_DISTRIBUTION_COMMON_LIBS_DIR = "common"
const val KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR = "platform"

const val KONAN_DISTRIBUTION_SOURCES_DIR = "sources"

fun konanCommonLibraryPath(libraryName: String) =
    File(KONAN_DISTRIBUTION_KLIB_DIR, KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryName)

fun konanPlatformLibraryPath(libraryName: String, platform: String) =
    File(KONAN_DISTRIBUTION_KLIB_DIR, KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(platform).resolve(libraryName)
