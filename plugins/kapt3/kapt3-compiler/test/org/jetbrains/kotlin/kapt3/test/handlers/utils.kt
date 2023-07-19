/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun Assertions.checkTxtAccordingToBackendAndFrontend(module: TestModule, actual: String, fileSuffix: String = "") {
    val testDataFile = module.files.first().originalFile
    val txtFile = testDataFile.withExtension("$fileSuffix.txt")
    val irTxtFile = testDataFile.withSuffixAndExtension("${fileSuffix}.ir", ".txt")
    val firTxtFile = testDataFile.withSuffixAndExtension("${fileSuffix}.fir", ".txt")
    val isFir = module.frontendKind == FrontendKinds.FIR
    val isIr = module.targetBackend?.isIR == true

    val expectedFile = when {
        isFir && firTxtFile.exists() -> firTxtFile
        isIr && irTxtFile.exists() -> irTxtFile
        else -> txtFile
    }

    assertEqualsToFile(expectedFile, actual)

    val classicText = txtFile.readText()
    val irText = runIf(irTxtFile.exists()) { irTxtFile.readText() }
    val firText = runIf(firTxtFile.exists()) { firTxtFile.readText() }

    if (isFir && firText != null) {
        if (firText == irText) {
            fail { "JVM_IR and FIR golden files are identical. Remove $firTxtFile." }
        } else if (firText == classicText && irText == null) {
            fail { "JVM and FIR golden files are identical. Remove $firTxtFile." }
        }
    } else if ((isIr || isFir) && irText != null && irText == classicText) {
        fail { "JVM and JVM_IR golden files are identical. Remove $irTxtFile." }
    }
}

private const val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
private val COMPLEX_KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()
private val SIMPLE_KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(.*\\)".toRegex()

fun removeMetadataAnnotationContents(s: String, complexCheck: Boolean): String {
    val regex = if (complexCheck) COMPLEX_KOTLIN_METADATA_REGEX else SIMPLE_KOTLIN_METADATA_REGEX
    return s.replace(regex, "@kotlin.Metadata()")
}
