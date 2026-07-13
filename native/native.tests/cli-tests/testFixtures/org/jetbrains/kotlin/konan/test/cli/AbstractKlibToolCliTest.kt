/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.cli

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.RegularKotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.copyNativeHomeProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.util.invokeKlibTool
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import java.io.File

abstract class AbstractKlibToolCliTest : AbstractCliTest() {
    private val kotlinNativeClassLoader by lazy {
        copyNativeHomeProperty()
        RegularKotlinNativeClassLoader.kotlinNativeClassLoader.classLoader
    }

    fun doKlibToolTest(fileName: String) {
        val filePath = ForTestCompileRuntime.transformTestDataPath(fileName).absolutePath
        val actual = invokeKlibTool(kotlinNativeClassLoader, args = readArgs(filePath, tmpdir.path)).second

        val outFile = File(filePath.replaceFirst("\\.args$".toRegex(), ".out"))
        assertEqualsToFile(outFile, actual)
    }
}
