/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.formatter.FormatSettingsUtil
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


/**
 * inspired by @see [org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase]
 */
abstract class AbstractPerformanceTypingIndentationTest : KotlinLightPlatformCodeInsightTestCase() {
    companion object {
        @JvmStatic
        val stats: Stats = Stats("typing-indentation")
    }

    protected fun doPerfTest(filePath: String) {
        val testName = getTestName(false)
        val testFileName = filePath.substring(0, filePath.indexOf("."))
        val testFileExtension = filePath.substring(filePath.lastIndexOf("."))
        val originFilePath = testFileName + testFileExtension
        val afterFilePath = "$testFileName.after$testFileExtension"
        val originalFileText = FileUtil.loadFile(File(originFilePath), true)

        try {
            val configurator = FormatSettingsUtil.createConfigurator(originalFileText, CodeStyle.getSettings(project))
            configurator.configureSettings()

            performanceTest<Unit, Unit> {
                name(testName)
                stats(stats)
                warmUpIterations(30)
                iterations(50)
                setUp {
                    configureByFile(originFilePath)
                }
                test {
                    executeAction(IdeActions.ACTION_EDITOR_ENTER)
                }
                tearDown {
                    val actualTextWithCaret = StringBuilder(editor.document.text).insert(
                        editor.caretModel.offset,
                        EditorTestUtil.CARET_TAG
                    ).toString()

                    KotlinTestUtils.assertEqualsToFile(File(afterFilePath), actualTextWithCaret)
                }
            }
        } finally {
            CodeStyle.getSettings(project).clearCodeStyleSettings()
        }
    }

    override fun getTestDataPath(): String = ""
}
