/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import platform.Foundation.*
import kotlin.test.*

private fun readString(path: String): String? {
    // we don't want actually read anything, just testing for compilability
    if (path != "") {
        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, error.ptr)
        }
    } else {
        return null
    }
}

fun main() {
    readString("")
}