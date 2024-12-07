/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists

internal fun Path.applyAndroidTestFixes() {
    // Path relative to the current gradle module project dir
    val keystoreFile = Paths.get("src/test/resources/common/debug.keystore")
    assert(keystoreFile.exists()) {
        "Common 'debug.keystore' file does not exists in ${keystoreFile.toAbsolutePath()} location!"
    }
    resolve("gradle.properties").also { if (!it.exists()) it.createFile() }.append(
        """
        |test.fixes.android.debugKeystore=${keystoreFile.toAbsolutePath().toString().normalizePath()}
        |
        """.trimMargin()
    )

    applyPlugin("org.jetbrains.kotlin.test.fixes.android", "org.jetbrains.kotlin:android-test-fixes", "test_fixes_version")
}