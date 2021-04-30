/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.common.kotlin

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.uast.UFile
import org.jetbrains.uast.asRecursiveLogString
import java.io.File

interface FirUastRenderLogTestBase : FirUastPluginSelection {
    fun getTestMetadataFileFromPath(filePath: String, ext: String): File {
        return File(filePath.removeSuffix(".kt") + '.' + ext)
    }

    private fun getRenderFile(filePath: String) = getTestMetadataFileFromPath(filePath, "render$pluginSuffix.txt")
    private fun getLogFile(filePath: String) = getTestMetadataFileFromPath(filePath, "log$pluginSuffix.txt")

    fun check(filePath: String, file: UFile) {
        val renderFile = getRenderFile(filePath)
        val logFile = getLogFile(filePath)

        KotlinTestUtils.assertEqualsToFile(renderFile, file.asRenderString())
        KotlinTestUtils.assertEqualsToFile(logFile, file.asRecursiveLogString())
    }
}
