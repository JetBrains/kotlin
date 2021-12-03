/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseGroup
import java.io.File

internal interface TestCaseGroupProvider {
    fun setPreprocessors(testDataDir: File, preprocessors: List<(String) -> String>)
    fun getTestCaseGroup(testDataDir: File): TestCaseGroup?
}

internal fun String.applySourceTransformers(sourceTransformers: List<(String) -> String>) =
    sourceTransformers.fold(this) { source, transformer -> transformer(source) }

internal fun File.applySourceTransformers(sourceTransformers: List<(String) -> String>) =
    readText().applySourceTransformers(sourceTransformers)
