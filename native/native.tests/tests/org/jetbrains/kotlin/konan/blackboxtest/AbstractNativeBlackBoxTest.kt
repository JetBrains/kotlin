/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.NativeBlackBoxTestSupport
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(NativeBlackBoxTestSupport::class)
abstract class AbstractNativeBlackBoxTest {
    internal lateinit var testRunProvider: TestRunProvider
    private val toBeRegistered = mutableListOf<Pair<File, List<(String) -> String>>>()
    internal fun onRunProviderSet() {
        for ((file, transformer) in toBeRegistered) {
            testRunProvider.setProcessors(file, transformer)
        }
    }

    fun register(@TestDataFile testDataFilePath: String, sourceTransformers: List<(String) -> String>) =
        toBeRegistered.add(File(testDataFilePath) to sourceTransformers)

    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: (String) -> String) =
        register(testDataFilePath, listOf(sourceTransformer))

    fun runTest(@TestDataFile testDataFilePath: String): Unit = with(testRunProvider) {
        val testDataFile = getAbsoluteFile(testDataFilePath)
        val testRun = getSingleTestRun(testDataFile)
        val testRunner = createRunner(testRun)
        testRunner.run()
    }
}
