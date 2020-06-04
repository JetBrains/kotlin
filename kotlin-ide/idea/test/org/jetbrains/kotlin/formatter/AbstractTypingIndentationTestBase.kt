/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractTypingIndentationTestBase : KotlinLightPlatformCodeInsightTestCase() {
    fun doNewlineTestWithInvert(afterInvFilePath: String) {
        doNewlineTest(afterInvFilePath, true)
    }

    @JvmOverloads
    fun doNewlineTest(afterFilePath: String, inverted: Boolean = false) {
        val testFileName = afterFilePath.substring(0, afterFilePath.indexOf("."))
        val testFileExtension = afterFilePath.substring(afterFilePath.lastIndexOf("."))
        val originFilePath = testFileName + testFileExtension
        val originalFileText = FileUtil.loadFile(File(originFilePath), true)
        try {
            val configurator = FormatSettingsUtil.createConfigurator(originalFileText, CodeStyle.getSettings(project))
            if (!inverted) {
                configurator.configureSettings()
            } else {
                configurator.configureInvertedSettings()
            }

            doNewlineTest(originFilePath, afterFilePath)
        } finally {
            CodeStyle.getSettings(project).clearCodeStyleSettings()
        }
    }

    private fun doNewlineTest(beforeFilePath: String, afterFilePath: String) {
        configureByFile(beforeFilePath)
        type('\n')
        val caretModel = editor.caretModel
        val offset = caretModel.offset
        val actualTextWithCaret = StringBuilder(editor.document.text).insert(offset, EditorTestUtil.CARET_TAG).toString()
        KotlinTestUtils.assertEqualsToFile(File(afterFilePath), actualTextWithCaret)
    }

    override fun getTestDataPath(): String = ""
}
