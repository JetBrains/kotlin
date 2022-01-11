/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import java.io.File

private val NORMAL_VERSION = 14
private val NORMAL_VERSION_FILE_NAME = "format-version.txt"

fun localCacheVersionManager(dataRoot: File, isCachesEnabled: Boolean) =
    CacheVersionManager(
        File(dataRoot, NORMAL_VERSION_FILE_NAME),
        if (isCachesEnabled) NORMAL_VERSION else null
    )