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

interface FirUastTypesTestBase : FirUastPluginSelection, FirUastFileComparisonTestBase {
    private fun getTypesFile(filePath: String, suffix: String): File = getTestMetadataFileFromPath(filePath, "types$suffix")

    private fun getIdenticalTypesFile(filePath: String): File = getTypesFile(filePath, TXT)

    private fun getPluginTypesFile(filePath: String): File {
        val identicalFile = getIdenticalTypesFile(filePath)
        if (identicalFile.exists()) return identicalFile
        return getTypesFile(filePath, "$pluginSuffix$TXT")
    }

    // TODO: ideally, we don't want this kind of whitelist.
    fun isExpectedToFail(filePath: String): Boolean {
        return false
    }

    fun check(filePath: String, file: UFile) {
        val typesFile = getPluginTypesFile(filePath)

        try {
            KotlinTestUtils.assertEqualsToFile(typesFile, file.asLogTypes())
            if (isExpectedToFail(filePath)) {
                KtAssert.fail("This test seems not fail anymore. Drop this from the white-list and re-run the test.")
            }
        } catch (e: Exception) {
            if (!isExpectedToFail(filePath)) throw e
        }

        cleanUpIdenticalFile(
            typesFile,
            getTypesFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalTypesFile(filePath),
            kind = "types"
        )
    }
}
