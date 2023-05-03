/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
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

fun expectTestToFailOnK2(test: () -> Unit) {
    val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 2.0") == true
    var testFailure: Throwable? = null
    try {
        test()
    } catch (e: Throwable) {
        testFailure = e
    }
    if (isK2 && testFailure == null) throw AssertionError("The test is expected to fail on K2")
    else if (!isK2 && testFailure != null) throw testFailure
}

