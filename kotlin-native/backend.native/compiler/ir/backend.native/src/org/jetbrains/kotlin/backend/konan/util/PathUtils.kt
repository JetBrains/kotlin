/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.bufferedWriter
import kotlin.io.path.pathString

fun Path.absoluteNormalizedPathString(): String = absolute().normalize().pathString

fun Path.printWriter(): PrintWriter = bufferedWriter().let(::PrintWriter)
