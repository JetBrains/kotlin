/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import java.nio.file.Path

// make sure it is file:// + absolute canonical path
fun Path.toCanonicalLocalFileUrlString(): String {
    return "file://" + toFile().canonicalPath
}

val isWindowsHost get() = System.getProperty("os.name").lowercase().contains("windows")