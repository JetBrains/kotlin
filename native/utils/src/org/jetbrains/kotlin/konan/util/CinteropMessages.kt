/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

// We store pretty cinterop messages because we want to access them in test infra as well.
object CInteropHints {
    // It would be nice to mark this property as const when `trimIndent` becomes constexpr.
    val fmodulesHint = """
            It seems that library is using clang modules. Try adding `-compiler-option -fmodules` to cinterop.
            For example, in case of cocoapods plugin:
            
            pod("PodName") {
                // Add these lines
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        """.trimIndent()
}
