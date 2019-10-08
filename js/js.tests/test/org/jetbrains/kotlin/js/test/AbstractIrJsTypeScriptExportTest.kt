/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.lang.Boolean.getBoolean

@Suppress("ConstantConditionIf")
abstract class AbstractIrJsTypeScriptExportTest : BasicIrBoxTest(
    pathToTestDir = TEST_DATA_DIR_PATH + "typescript-export/",
    testGroupOutputDirPrefix = "typescript-export/",
    pathToRootOutputDir = TEST_DATA_DIR_PATH
) {
    override val generateDts = true
    private val updateReferenceDtsFiles = getBoolean("kotlin.js.updateReferenceDtsFiles")

    override fun performAdditionalChecks(inputFile: File, outputFile: File) {
        val referenceDtsFile = inputFile.withReplacedExtensionOrNull(".kt", ".d.ts")
            ?: error("Can't find reference .d.ts file")
        val generatedDtsFile = outputFile.withReplacedExtensionOrNull("_v5", ".d.ts")
            ?: error("Can't find generated .d.ts file")

        val generatedDts = generatedDtsFile.readText()

        if (updateReferenceDtsFiles)
            referenceDtsFile.writeText(generatedDts)
        else
            KotlinTestUtils.assertEqualsToFile(referenceDtsFile, generatedDts)
    }
}