/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

internal fun Path.filterKotlinFusFiles() =
    this.listDirectoryEntries().filter { it.name.endsWith(".kotlin-profile") || it.name.endsWith(".plugin-profile") }

internal fun Path.filterBackwardCompatibilityKotlinFusFiles() = this.listDirectoryEntries().filter { it.name.endsWith(".profile") }
