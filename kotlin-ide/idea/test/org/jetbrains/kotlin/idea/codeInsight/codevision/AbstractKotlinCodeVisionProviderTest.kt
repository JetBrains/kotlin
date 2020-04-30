/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import org.jetbrains.kotlin.idea.codeInsight.codevision.KotlinCodeVisionProvider.KotlinCodeVisionSettings
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

open class AbstractKotlinCodeVisionProviderTest : InlayHintsProviderTestCase() { // Abstract- prefix is just a convention for GenerateTests

    fun doTest(testPath: String) { // named according to the convention imposed by GenerateTests
        assertThatActualHintsMatch(testPath)
    }

    private fun assertThatActualHintsMatch(fileName: String) {
        val fileContents = FileUtil.loadFile(File(fileName), true)

        val usagesLimit = InTextDirectivesUtils.findStringWithPrefixes(fileContents, "// USAGES-LIMIT: ")?.toInt() ?: 100
        val inheritorsLimit = InTextDirectivesUtils.findStringWithPrefixes(fileContents, "// INHERITORS-LIMIT: ")?.toInt() ?: 100

        val codeVisionProvider = KotlinCodeVisionProvider()
        codeVisionProvider.usagesLimit = usagesLimit
        codeVisionProvider.inheritorsLimit = inheritorsLimit

        val mode: KotlinCodeVisionSettings = when (InTextDirectivesUtils.findStringWithPrefixes(fileContents, "// MODE: ")) {
            "inheritors" -> inheritorsEnabled()
            "usages" -> usagesEnabled()
            "usages-&-inheritors" -> usagesAndInheritorsEnabled()
            else -> codeVisionDisabled()
        }

        testProvider("kotlinCodeVision.kt", fileContents, codeVisionProvider, mode)
    }

    private fun usagesAndInheritorsEnabled(): KotlinCodeVisionSettings = KotlinCodeVisionSettings(showUsages = true, showInheritors = true)

    private fun inheritorsEnabled(): KotlinCodeVisionSettings = KotlinCodeVisionSettings(showUsages = false, showInheritors = true)

    private fun usagesEnabled(): KotlinCodeVisionSettings = KotlinCodeVisionSettings(showUsages = true, showInheritors = false)

    private fun codeVisionDisabled(): KotlinCodeVisionSettings = KotlinCodeVisionSettings(showUsages = false, showInheritors = false)
}
