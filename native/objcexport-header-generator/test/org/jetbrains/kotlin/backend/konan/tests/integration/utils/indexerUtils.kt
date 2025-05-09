/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import kotlinx.cinterop.JvmCInteropCallbacks
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.Xcode
import org.jetbrains.kotlin.utils.NativeMemoryAllocator

private val xcode = Xcode.Companion.findCurrent()
internal val appleSdkPath = xcode.macosxSdk
internal val appleFrameworkPath = "$appleSdkPath/System/Library/Frameworks"

internal fun initIndexerUtils() {
    NativeMemoryAllocator.Companion.init()
    JvmCInteropCallbacks.init()
}

internal fun disposeIndexerUtils() {
    JvmCInteropCallbacks.dispose()
    NativeMemoryAllocator.Companion.dispose()
}

internal fun getClangResourceDir(): String {
    val clangPath = Command("/usr/bin/xcrun", "-f", "clang").getOutputLines().first()
    val resourceDir = Command(clangPath, "--print-resource-dir").getOutputLines().first()
    return "$resourceDir/include"
}