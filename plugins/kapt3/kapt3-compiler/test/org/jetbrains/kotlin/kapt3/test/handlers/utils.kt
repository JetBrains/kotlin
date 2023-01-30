/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension

fun Assertions.checkTxtAccordingToBackend(module: TestModule, actual: String, fileSuffix: String = "") {
    val testDataFile = module.files.first().originalFile
    val txtFile = testDataFile.withExtension("$fileSuffix.txt")
    val irTxtFile = testDataFile.withSuffixAndExtension("$fileSuffix.ir", ".txt")
    val isIr = module.targetBackend?.isIR == true
    val expectedFile = if (isIr && irTxtFile.exists()) {
        irTxtFile
    } else {
        txtFile
    }
    assertEqualsToFile(expectedFile, actual)

    if (isIr && txtFile.exists() && irTxtFile.exists() && txtFile.readText() == irTxtFile.readText()) {
        fail { "JVM and JVM_IR golden files are identical. Remove $irTxtFile." }
    }
}

private const val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()

fun removeMetadataAnnotationContents(s: String): String {
    return s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
}
