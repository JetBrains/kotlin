/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import java.nio.file.Path
import java.nio.file.Paths

const val KLIB_FILE_EXTENSION = "klib"
const val KLIB_FILE_EXTENSION_WITH_DOT = ".$KLIB_FILE_EXTENSION"

const val KLIB_METADATA_FILE_EXTENSION = "knm"
const val KLIB_METADATA_FILE_EXTENSION_WITH_DOT = ".$KLIB_METADATA_FILE_EXTENSION"

const val KLIB_MODULE_METADATA_FILE_NAME = "module"

const val KONAN_STDLIB_NAME = "stdlib"

val KONAN_COMMON_LIBS_PATH: Path
    get() = Paths.get("klib", "common")

val KONAN_ALL_PLATFORM_LIBS_PATH: Path
    get() = Paths.get("klib", "platform")

fun konanCommonLibraryPath(libraryName: String): Path = KONAN_COMMON_LIBS_PATH.resolve(libraryName)

fun konanSpecificPlatformLibrariesPath(platform: String): Path = KONAN_ALL_PLATFORM_LIBS_PATH.resolve(platform)

fun konanPlatformLibraryPath(platform: String, libraryName: String): Path = konanSpecificPlatformLibrariesPath(platform).resolve(libraryName)
