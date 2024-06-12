/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.playground

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.test.KotlinTestUtils
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

abstract class AbstractPlaygroundTranslatorTest {
    protected fun runTest(@TestDataFile testPath: String) {
        val sourceFile = Path(testPath)
        val actual = PlaygroundTranslator(Path(KotlinNativeDistribution.stdlibPath)).translate(sourceFile)
        val expectedFile = sourceFile.resolveSibling("${sourceFile.nameWithoutExtension}.swift")
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
    }
}

private object KotlinNativeDistribution {
    val stdlibPath: String
        get() = Distribution(System.getProperty("kotlin.internal.native.test.nativeHome")).stdlib
}
