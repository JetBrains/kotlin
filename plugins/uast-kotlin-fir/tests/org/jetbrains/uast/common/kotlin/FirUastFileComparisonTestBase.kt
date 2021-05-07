/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.common.kotlin

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

interface FirUastFileComparisonTestBase {
    fun getTestMetadataFileFromPath(filePath: String, ext: String): File {
        return File(filePath.removeSuffix(".kt") + '.' + ext)
    }

    private val isTeamCityBuild: Boolean
        get() = System.getenv("TEAMCITY_VERSION") != null
                || KtUsefulTestCase.IS_UNDER_TEAMCITY

    fun cleanUpIdenticalFile(
        currentFile: File,
        counterpartFile: File,
        identicalFile: File,
        kind: String
    ) {
        // Already cleaned up
        if (identicalFile.exists()) return
        // Nothing to compare
        if (!currentFile.exists() || !counterpartFile.exists()) return

        val content = currentFile.readText().trim()
        if (content == counterpartFile.readText().trim()) {
            val message = if (isTeamCityBuild) {
                "Please remove .$kind.fir.txt dump and .$kind.fe10.txt dump"
            } else {
                currentFile.delete()
                counterpartFile.delete()
                FileUtil.writeToFile(identicalFile, content)
                "Deleted .$kind.fir.txt dump and .$kind.fe10.txt dump, added .$kind.txt instead"
            }
            KtAssert.fail(
                """
                    Dump via FIR UAST & via FE10 UAST are the same.
                    $message
                    Please re-run the test now
                """.trimIndent()
            )
        }
    }
}
