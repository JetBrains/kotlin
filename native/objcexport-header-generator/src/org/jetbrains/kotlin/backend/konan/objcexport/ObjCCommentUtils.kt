/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

@InternalKotlinNativeApi
operator fun ObjCComment?.plus(other: ObjCComment?): ObjCComment? {
    val lines = this?.contentLines.orEmpty() + other?.contentLines.orEmpty()
    if (lines.isEmpty()) return null
    return ObjCComment(lines)
}

@InternalKotlinNativeApi
operator fun ObjCComment?.plus(lines: Iterable<String>): ObjCComment? {
    val newLines = this?.contentLines.orEmpty() + lines
    if (newLines.isEmpty()) return null
    return ObjCComment(newLines)
}
