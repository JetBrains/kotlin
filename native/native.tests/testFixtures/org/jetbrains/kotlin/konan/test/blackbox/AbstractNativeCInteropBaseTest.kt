/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader

abstract class AbstractNativeCInteropBaseTest : AbstractNativeSimpleTest() {
    internal val kotlinNativeClassLoader: KotlinNativeClassLoader get() = testRunSettings.get()

    protected fun normalizeCSymbolNames(metadataDump: String): String =
        if (targets.testTarget.family.isAppleFamily) {
            // Remove '_' prefix from @CCall.Direct and @CGlobalAccess name values on Apple targets.
            // It is added to symbol names there by default, unlike all other targets.
            // This little hack allows us to keep using the same "golden file" for all targets.
            metadataDump.replace(
                "@kotlinx/cinterop/internal/CCall.Direct(name = \"_",
                "@kotlinx/cinterop/internal/CCall.Direct(name = \""
            ).replace(
                "@kotlinx/cinterop/internal/CGlobalAccess(name = \"_",
                "@kotlinx/cinterop/internal/CGlobalAccess(name = \""
            )
        } else {
            metadataDump
        }
}
