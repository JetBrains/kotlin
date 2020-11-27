/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.checkers.utils.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.codeMetaInfo.CodeMetaInfoTestCase
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.LineMarkerRenderConfiguration
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.Test
import java.io.File


class ImportAndCheckHighlighting : MultiplePluginVersionGradleImportingTestCase() {
    @Test
    @PluginTargetVersions(pluginVersion = "1.3.40+")
    fun testMultiplatformLibrary() {
        importAndCheckHighlighting(checkLineMarkers = true)
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.40+")
    fun testUnresolvedInMultiplatformLibrary() {
        importAndCheckHighlighting(checkWarnings = true)
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.40+")
    fun testMacosTargets() {
        importAndCheckHighlighting(checkLineMarkers = true)
    }

    private fun importAndCheckHighlighting(checkLineMarkers: Boolean = false, checkWarnings: Boolean = false) {
        val files = configureByFiles()
        importProject()
        val project = myTestFixture.project
        val configurations = listOfNotNull(
            LineMarkerRenderConfiguration().takeIf { checkLineMarkers },
            DiagnosticCodeMetaInfoRenderConfiguration().takeIf { checkWarnings }
        )

        val checker = CodeMetaInfoTestCase(configurations, false)

        files.filter { it.extension == "kt" || it.extension == "java" }.forEach { file ->
            val expectedFile = File(file.getUserData(VfsTestUtil.TEST_DATA_FILE_PATH)!!)
            runInEdtAndWait {
                checker.checkFile(file, expectedFile, project)
            }
        }
    }

    override fun testDataDirName() = "importAndCheckHighlighting"

    override fun clearTextFromMarkup(text: String) = clearTextFromDiagnosticMarkup(text)
}
