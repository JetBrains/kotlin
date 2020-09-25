/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.IOException

abstract class AbstractEnterAfterUnmatchedBraceHandlerTest : KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        private const val FILE_SEPARATOR = "//-----"
    }

    protected fun doTest(path: String) {
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

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) return KotlinLightProjectDescriptor.INSTANCE
        return try {
            val fileText = FileUtil.loadFile(File(testDataPath, fileName()), true)
            if (InTextDirectivesUtils.isDirectiveDefined(fileText, "WITH_RUNTIME")) {
                KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
            } else {
                JAVA_LATEST
            }
        } catch (e: IOException) {
            throw rethrow(e)
        }
    }
}
