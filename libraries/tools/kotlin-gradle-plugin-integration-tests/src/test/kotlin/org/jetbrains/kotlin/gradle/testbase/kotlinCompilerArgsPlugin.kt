/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Path

fun Path.applyKotlinCompilerArgsPlugin() {
    applyPlugin(
        "org.jetbrains.kotlin.test.kotlin-compiler-args-properties",
        "org.jetbrains.kotlin:kotlin-compiler-args-properties",
        "test_fixes_version"
    )
}