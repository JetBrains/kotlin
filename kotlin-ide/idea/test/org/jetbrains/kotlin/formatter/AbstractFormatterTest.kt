/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.LightIdeaTestCase
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ThrowableRunnable
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.test.configureCodeStyleAndRun
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

// Based on from com.intellij.psi.formatter.java.AbstractJavaFormatterTest
abstract class AbstractFormatterTest : LightIdeaTestCase() {
    enum class Action {
        REFORMAT, INDENT
    }

    private interface TestFormatAction {
        fun run(psiFile: PsiFile, startOffset: Int, endOffset: Int)
    }

    companion object {
        private val ACTIONS: Map<Action, TestFormatAction> = mapOf(
            Action.REFORMAT to object : TestFormatAction {
                override fun run(psiFile: PsiFile, startOffset: Int, endOffset: Int) {
                    CodeStyleManager.getInstance(psiFile.project).reformatText(psiFile, startOffset, endOffset)
                }
            },

            Action.INDENT to object : TestFormatAction {
                override fun run(psiFile: PsiFile, startOffset: Int, endOffset: Int) {
                    CodeStyleManager.getInstance(psiFile.project).adjustLineIndent(psiFile, startOffset)
                }
            },
        )
    }

    override fun setUp() {
        super.setUp()
        LanguageLevelProjectExtension.getInstance(project).languageLevel = LanguageLevel.HIGHEST
        Registry.get("kotlin.formatter.allowTrailingCommaInAnyProject").setValue(true)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { Registry.get("kotlin.formatter.allowTrailingCommaInAnyProject").resetToDefault() },
            ThrowableRunnable { super.tearDown() }
        )
    }

    fun doTextTest(@NonNls text: String, fileAfter: File?, extension: String) {
        doTextTest(Action.REFORMAT, text, fileAfter, extension)
    }

    fun doTextTest(action: Action, text: String, fileAfter: File?, extension: String) {
        val file = createFile("A$extension", text)
        val manager = PsiDocumentManager.getInstance(project)
        val document = manager.getDocument(file) ?: error("Don't expect the document to be null")
        project.executeWriteCommand("") {
            document.replaceString(0, document.textLength, text)
            manager.commitDocument(document)
            try {
                val rangeToUse = file.textRange
                ACTIONS[action]?.run(file, rangeToUse.startOffset, rangeToUse.endOffset)
            } catch (e: IncorrectOperationException) {
                fail(e.localizedMessage)
            }
        }

        KotlinTestUtils.assertEqualsToFile(fileAfter!!, document.text)
        manager.commitDocument(document)
        KotlinTestUtils.assertEqualsToFile(fileAfter, file.text)
    }

    fun doTestInverted(expectedFileNameWithExtension: String) {
        doTest(expectedFileNameWithExtension, true, false)
    }

    fun doTestInvertedCallSite(expectedFileNameWithExtension: String) {
        doTest(expectedFileNameWithExtension, true, false)
    }

    fun doTestCallSite(expectedFileNameWithExtension: String) {
        doTest(expectedFileNameWithExtension, false, true)
    }

    @JvmOverloads
    fun doTest(expectedFileNameWithExtension: String, inverted: Boolean = false, callSite: Boolean = false) {
        val testFileName = expectedFileNameWithExtension.substring(0, expectedFileNameWithExtension.indexOf("."))
        val testFileExtension = expectedFileNameWithExtension.substring(expectedFileNameWithExtension.lastIndexOf("."))
        val originalFileText = FileUtil.loadFile(File(testFileName + testFileExtension), true)

        configureCodeStyleAndRun(project) {
            val codeStyleSettings = CodeStyle.getSettings(project)
            val customSettings = codeStyleSettings.kotlinCustomSettings
            val rightMargin = InTextDirectivesUtils.getPrefixedInt(originalFileText, "// RIGHT_MARGIN: ")
            if (rightMargin != null) {
                codeStyleSettings.setRightMargin(KotlinLanguage.INSTANCE, rightMargin)
            }

            val trailingComma = InTextDirectivesUtils.getPrefixedBoolean(originalFileText, "// TRAILING_COMMA: ")
            if (trailingComma != null) {
                customSettings.ALLOW_TRAILING_COMMA = trailingComma
            }

            val configurator = FormatSettingsUtil.createConfigurator(originalFileText, codeStyleSettings)
            if (!inverted) {
                configurator.configureSettings()
            } else {
                configurator.configureInvertedSettings()
            }

            customSettings.ALLOW_TRAILING_COMMA_ON_CALL_SITE = callSite
            doTextTest(originalFileText, File(expectedFileNameWithExtension), testFileExtension)
        }
    }
}