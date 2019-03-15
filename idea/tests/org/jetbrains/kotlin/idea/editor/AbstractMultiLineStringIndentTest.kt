/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.SettingsConfigurator
import java.io.File

abstract class AbstractMultiLineStringIndentTest : KotlinLightCodeInsightFixtureTestCase() {
    val FILE_SEPARATOR = "//-----"

    protected fun doTest(path: String) {
        val testDir = File(path).parentFile
        val settingsFile = File(testDir, "settings.txt")

        setupSettings(settingsFile) {
            val multiFileText = FileUtil.loadFile(File(path), true)

            val beforeFile = multiFileText.substringBefore(FILE_SEPARATOR).trim()
            val afterFile = multiFileText.substringAfter(FILE_SEPARATOR).trim()

            myFixture.setCaresAboutInjection(false)
            myFixture.configureByText(KotlinFileType.INSTANCE, beforeFile)
            myFixture.type('\n')

            val caretModel = myFixture.editor.caretModel
            val offset = caretModel.offset
            val actualTextWithCaret = StringBuilder(myFixture.editor.document.text).insert(offset, EditorTestUtil.CARET_TAG).toString()

            if (afterFile != actualTextWithCaret) {
                KotlinTestUtils.assertEqualsToFile(File(path), "$beforeFile\n$FILE_SEPARATOR\n$actualTextWithCaret")
            }
        }
    }

    fun setupSettings(settingsFile: File, inverted: Boolean = false, action: () -> Unit) {
        if (!settingsFile.exists()) {
            // Don nothing with settings
            action()
            return
        }

        val settingsFileText = FileUtil.loadFile(settingsFile, true)
        try {
            val indentOptions = CodeStyleSettingsManager.getInstance(project).currentSettings.getIndentOptions(KotlinFileType.INSTANCE)
            val configurator = SettingsConfigurator(settingsFileText, indentOptions)
            if (!inverted) {
                configurator.configureSettings()
            }
            else {
                configurator.configureInvertedSettings()
            }

            action()
        }
        finally {
            CodeStyleSettingsManager.getSettings(myFixture.project).clearCodeStyleSettings()
        }
    }

    override fun getProjectDescriptor() = JAVA_LATEST!!
    override fun getTestDataPath(): String = ""
}
