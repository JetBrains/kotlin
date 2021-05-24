/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import org.jetbrains.kotlin.test.KotlinTestUtils
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

    fun check(filePath: String, file: UFile) {
        val typesFile = getPluginTypesFile(filePath)

        KotlinTestUtils.assertEqualsToFile(typesFile, file.asLogTypes())

        cleanUpIdenticalFile(
            typesFile,
            getTypesFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalTypesFile(filePath),
            kind = "types"
        )
    }
}
