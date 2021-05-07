/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.common.kotlin

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.uast.UComment
import org.jetbrains.uast.UFile
import org.jetbrains.uast.asRecursiveLogString
import org.jetbrains.uast.common.kotlin.FirUastTestSuffix.TXT
import org.jetbrains.uast.kotlin.internal.KotlinUElementWithComments
import java.io.File

interface FirUastCommentLogTestBase : FirUastPluginSelection, FirUastFileComparisonTestBase {
    private fun getCommentsFile(filePath: String, suffix: String): File = getTestMetadataFileFromPath(filePath, "comments$suffix")

    private fun getIdenticalCommentsFile(filePath: String): File = getCommentsFile(filePath, TXT)

    private fun getPluginCommentsFile(filePath: String): File {
        val identicalFile = getIdenticalCommentsFile(filePath)
        if (identicalFile.exists()) return identicalFile
        return getCommentsFile(filePath, "$pluginSuffix$TXT")
    }

    private fun UComment.testLog(): String {
        return "UComment(${text})"
    }

    fun check(filePath: String, file: UFile) {
        val commentsFile = getPluginCommentsFile(filePath)

        val comments = file.asRecursiveLogString { uElement ->
            val stringBuilder = StringBuilder()
            when (uElement) {
                is UFile -> {
                    if (uElement.allCommentsInFile.isNotEmpty()) {
                        stringBuilder.append("UFile(allCommentsInFile:\n")
                        stringBuilder.append(uElement.allCommentsInFile.joinToString(separator = "\n") { it.testLog() })
                        stringBuilder.append("\n)")
                    }
                }
                is KotlinUElementWithComments -> {
                    if (uElement.comments.isNotEmpty()) {
                        stringBuilder.append("${uElement::class.java.simpleName}(\n")
                        uElement.comments.joinToString(separator = "\n") { it.testLog() }
                        stringBuilder.append("\n)")
                    }
                }
            }
            stringBuilder.toString()
        }
        // No comments in the file
        if (comments.isEmpty()) return

        KotlinTestUtils.assertEqualsToFile(commentsFile, comments)

        cleanUpIdenticalFile(
            commentsFile,
            getCommentsFile(filePath, "$counterpartSuffix$TXT"),
            getIdenticalCommentsFile(filePath),
            kind = "comments"
        )
    }
}
