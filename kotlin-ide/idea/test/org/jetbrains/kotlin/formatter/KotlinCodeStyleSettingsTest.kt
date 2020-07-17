/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.psi.codeStyle.CodeStyleScheme
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.impl.source.codeStyle.json.CodeStyleSchemeJsonExporter
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.formatter.*
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.ByteArrayOutputStream
import java.io.File


class KotlinCodeStyleSettingsTest : LightPlatformTestCase() {
    fun `test json export with official code style`() = doTestWithJson(KotlinStyleGuideCodeStyle.INSTANCE, "officialCodeStyle")
    fun `test json export with obsolete code style`() = doTestWithJson(KotlinObsoleteCodeStyle.INSTANCE, "obsoleteCodeStyle")
    fun `test compare code styles`() = compareCodeStyle { KotlinStyleGuideCodeStyle.apply(it) }
    fun `test compare different import`() = compareCodeStyle {
        it.kotlinCustomSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(KotlinPackageEntry("not.my.package.name", true))
        it.kotlinCommonSettings.BRACE_STYLE = 10
    }

    fun `test compare different layout`() = compareCodeStyle {
        it.kotlinCustomSettings.PACKAGES_IMPORT_LAYOUT.addEntry(KotlinPackageEntry("my.package.name", true))
        it.kotlinCommonSettings.BRACE_STYLE = 10
    }

    private fun compareCodeStyle(transformer: (CodeStyleSettings) -> Unit) {
        val settings = CodeStyle.getSettings(project)
        val copyOfSettings = CodeStyleSettingsManager.getInstance().cloneSettings(settings)

        assertTrue(settings.kotlinCommonSettings == settings.kotlinCommonSettings)
        assertTrue(settings.kotlinCustomSettings == settings.kotlinCustomSettings)
        assertTrue(copyOfSettings.kotlinCommonSettings == copyOfSettings.kotlinCommonSettings)
        assertTrue(copyOfSettings.kotlinCustomSettings == copyOfSettings.kotlinCustomSettings)
        assertTrue(settings.kotlinCommonSettings == copyOfSettings.kotlinCommonSettings)
        assertTrue(copyOfSettings.kotlinCustomSettings == settings.kotlinCustomSettings)

        transformer(copyOfSettings)

        assertFalse(settings.kotlinCommonSettings == copyOfSettings.kotlinCommonSettings)
        assertFalse(settings.kotlinCustomSettings == copyOfSettings.kotlinCustomSettings)

        assertTrue(copyOfSettings.kotlinCommonSettings == copyOfSettings.kotlinCommonSettings)
        assertTrue(copyOfSettings.kotlinCustomSettings == copyOfSettings.kotlinCustomSettings)
    }
}

private fun doTestWithJson(codeStyle: KotlinPredefinedCodeStyle, fileName: String) {
    val jsonScheme = File(IDEA_TEST_DATA_DIR, "codeStyle/$fileName.json")
    assert(jsonScheme.exists())

    val testScheme = createTestScheme()
    val settings = testScheme.codeStyleSettings
    codeStyle.apply(settings)

    val kotlinCustomCodeStyle = settings.kotlinCustomSettings
    kotlinCustomCodeStyle.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(KotlinPackageEntry("java.util", false))
    kotlinCustomCodeStyle.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(KotlinPackageEntry("kotlinx.android.synthetic", true))
    kotlinCustomCodeStyle.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(KotlinPackageEntry("io.ktor", true))

    val exporter = CodeStyleSchemeJsonExporter()
    val outputStream = ByteArrayOutputStream()
    exporter.exportScheme(testScheme, outputStream, listOf("kotlin"))
    KotlinTestUtils.assertEqualsToFile(jsonScheme, outputStream.toString())
}

private fun createTestScheme() = object : CodeStyleScheme {
    private val mySettings = CodeStyle.createTestSettings()
    override fun getName(): String = "Test"

    override fun isDefault(): Boolean = false

    override fun getCodeStyleSettings(): CodeStyleSettings = mySettings
}