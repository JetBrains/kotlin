/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.common.kotlin.FirUastTestSuffix.TXT
import java.io.File

interface FirUastValuesTestBase : FirUastPluginSelection, FirUastFileComparisonTestBase {
    private fun getValuesFile(filePath: String, suffix: String): File = getTestMetadataFileFromPath(filePath, "values$suffix")

    private fun getIdenticalValuesFile(filePath: String): File = getValuesFile(filePath, TXT)

    private fun getPluginValuesFile(filePath: String): File {
        val identicalFile = getIdenticalValuesFile(filePath)
        if (identicalFile.exists()) return identicalFile
        return getValuesFile(filePath, "$pluginSuffix$TXT")
    }

    // TODO: ideally, we don't want this kind of whitelist.
    fun isExpectedToFail(filePath: String): Boolean {
        return false
    }

    fun check(filePath: String, file: UFile) {
        val valuesFile = getPluginValuesFile(filePath)

        try {
            KotlinTestUtils.assertEqualsToFile(valuesFile, file.asLogValues())
            if (isExpectedToFail(filePath)) {
                KtAssert.fail("This test seems not fail anymore. Drop this from the white-list and re-run the test.")
            }
        } catch (e: Exception) {
            if (!isExpectedToFail(filePath)) throw e
        }

        cleanUpIdenticalFile(
            valuesFile,
            getValuesFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalValuesFile(filePath),
            kind = "values"
        )
    }
}
