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

interface FirUastIdentifiersTestBase : FirUastPluginSelection, FirUastFileComparisonTestBase {
    private fun getIdentifiersFile(filePath: String, suffix: String): File = getTestMetadataFileFromPath(filePath, "identifiers$suffix")
    private fun getRefNamesFile(filePath: String, suffix: String): File = getTestMetadataFileFromPath(filePath, "refNames$suffix")

    private fun getIdenticalIdentifiersFile(filePath: String): File = getIdentifiersFile(filePath, TXT)
    private fun getIdenticalRefNamesFile(filePath: String): File = getRefNamesFile(filePath, TXT)

    private fun getPluginIdentifiersFile(filePath: String): File {
        val identicalFile = getIdenticalIdentifiersFile(filePath)
        if (identicalFile.exists()) return identicalFile
        return getIdentifiersFile(filePath, "$pluginSuffix${TXT}")
    }

    private fun getPluginRefNamesFile(filePath: String): File {
        val identicalFile = getIdenticalRefNamesFile(filePath)
        if (identicalFile.exists()) return identicalFile
        return getRefNamesFile(filePath, "$pluginSuffix${TXT}")
    }

    // TODO: ideally, we don't want this kind of whitelist.
    fun isExpectedToFail(filePath: String): Boolean {
        return false
    }

    fun check(filePath: String, file: UFile) {
        val identifiersFile = getPluginIdentifiersFile(filePath)
        val refNamesFile = getPluginRefNamesFile(filePath)

        val identifiersContent = file.asIdentifiersWithParents().trim()
        if (identifiersContent.isNotEmpty()) {
            KotlinTestUtils.assertEqualsToFile(identifiersFile, identifiersContent)
        }
        val refNamesContent = file.asRefNames().trim()
        if (refNamesContent.isNotEmpty()) {
            KotlinTestUtils.assertEqualsToFile(refNamesFile, refNamesContent)
        }

        try {
            file.testIdentifiersParents()
            if (isExpectedToFail(filePath)) {
                KtAssert.fail("This test seems not fail anymore. Drop this from the white-list and re-run the test.")
            }
        } catch (e: AssertionError) {
            if (!isExpectedToFail(filePath)) throw e
        }

        cleanUpIdenticalFile(
            identifiersFile,
            getIdentifiersFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalIdentifiersFile(filePath),
            kind = "identifiers"
        )
        cleanUpIdenticalFile(
            refNamesFile,
            getRefNamesFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalRefNamesFile(filePath),
            kind = "refNames"
        )
    }
}
