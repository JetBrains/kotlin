/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.test.runAll
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.formatter.FormatSettingsUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.configureCodeStyleAndRun
import org.jetbrains.kotlin.idea.testFramework.dispatchAllInvocationEvents
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


/**
 * inspired by @see [org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase]
 */
abstract class AbstractPerformanceTypingIndentationTest : KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        @JvmStatic
        val stats: Stats = Stats("typing-indentation")
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable { stats.flush() }
        )
    }

    protected fun doPerfTest(unused: String) {
        val testName = getTestName(false)

        myFixture.testDataPath = testDataPath
        val testDataFile = testDataFile()
        val filePath = testDataFile.path
        val testFileName = filePath.substring(0, filePath.indexOf("."))
        val testFileExtension = filePath.substring(filePath.lastIndexOf("."))
        val afterFilePath = "$testFileName.after$testFileExtension"
        val originalFileText = FileUtil.loadFile(testDataFile, true)

        configureCodeStyleAndRun(
            project,
            configurator = { FormatSettingsUtil.createConfigurator(originalFileText, it).configureSettings() }) {
            performanceTest<Unit, Unit> {
                name(testName)
                stats(stats)
                warmUpIterations(20)
                iterations(30)
                setUp {
                    myFixture.configureByFile(testDataFile.name)
                }
                test {
                    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
                    dispatchAllInvocationEvents()
                }
                tearDown {

                    val actualTextWithCaret = StringBuilder(editor.document.text).insert(
                        editor.caretModel.offset,
                        EditorTestUtil.CARET_TAG
                    ).toString()

                    // to avoid VFS refresh
                    myFixture.performEditorAction(IdeActions.ACTION_UNDO)

                    KotlinTestUtils.assertEqualsToFile(File(afterFilePath), actualTextWithCaret)
                }
            }
        }
    }

}