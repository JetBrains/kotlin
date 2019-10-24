/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import java.io.File
import java.nio.file.Files

internal const val TEST_DATA_DIR = "libraries/scripting/jvm-host-test/testData"

internal fun <R> withTempDir(keyName: String = "tmp", body: (File) -> R) {
    val tempDir = Files.createTempDirectory(keyName).toFile()
    try {
        body(tempDir)
    } finally {
        tempDir.deleteRecursively()
    }
}

