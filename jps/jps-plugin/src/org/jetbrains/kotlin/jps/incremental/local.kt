/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import java.nio.file.Path

private const val NORMAL_VERSION = 14
private const val NORMAL_VERSION_FILE_NAME = "format-version.txt"

fun localCacheVersionManager(dataRoot: Path, isCachesEnabled: Boolean) = CacheVersionManager(
    dataRoot.resolve(NORMAL_VERSION_FILE_NAME),
    if (isCachesEnabled) NORMAL_VERSION else null
)