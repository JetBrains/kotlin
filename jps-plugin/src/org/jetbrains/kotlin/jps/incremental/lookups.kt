/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import java.io.File

private val DATA_CONTAINER_VERSION_FILE_NAME = "data-container-format-version.txt"
private val DATA_CONTAINER_VERSION = 5

fun lookupsCacheVersionManager(dataRoot: File, isEnabled: Boolean) =
    CacheVersionManager(
        File(dataRoot, DATA_CONTAINER_VERSION_FILE_NAME),
        if (isEnabled) DATA_CONTAINER_VERSION else null
    )