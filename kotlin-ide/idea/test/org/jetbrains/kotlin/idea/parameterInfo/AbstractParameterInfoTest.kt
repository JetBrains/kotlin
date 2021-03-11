/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hint.ShowParameterInfoContext
import com.intellij.codeInsight.hint.ShowParameterInfoHandler
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.slashedPath
import org.junit.Assert
import java.io.File

abstract class AbstractParameterInfoTest : LightJavaCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        val root = KotlinTestUtils.getTestsRoot(this::class.java)
        if (root.contains("Lib")) {
            return SdkAndMockLibraryProjectDescriptor("$root/sharedLib", true, true, false, false)
        }

        return ProjectDescriptorWithStdlibSources.INSTANCE
    }

    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = IDEA_TEST_DATA_DIR.resolve("parameterInfo").slashedPath
    }

    protected fun doTest(fileName: String) {
        val prefix = FileUtil.getNameWithoutExtension(PathUtil.getFileName(fileName))
        val mainFile = File(FileUtil.toSystemDependentName(fileName))
        mainFile.parentFile
            .listFiles { _, name ->
                name.startsWith("$prefix.") &&
                        name != mainFile.name &&
                        name.substringAfterLast(".") in setOf("java", "kt")
            }!!
            .forEach { myFixture.configureByFile(it.absolutePath.substringAfter(myFixture.testDataPath)) }

        myFixture.configureByFile(File(fileName).absolutePath.substringAfter(myFixture.testDataPath))

        val file = myFixture.file as KtFile

        withCustomCompilerOptions(file.text, project, myFixture.module) {
            val lastChild = file.allChildren.filter { it !is PsiWhiteSpace }.last()
            val expectedResultText = when (lastChild.node.elementType) {
                KtTokens.BLOCK_COMMENT -> lastChild.text.substring(2, lastChild.text.length - 2).trim()
                KtTokens.EOL_COMMENT -> lastChild.text.substring(2).trim()
                else -> error("Unexpected last file child")
            }

            val context = ShowParameterInfoContext(editor, project, file, editor.caretModel.offset, -1, true)

            val handlers = ShowParameterInfoHandler.getHandlers(project, KotlinLanguage.INSTANCE)!!
            val handler = handlers.firstOrNull { it.findElementForParameterInfo(context) != null }
                          ?: error("Could not find parameter info handler")

            val mockCreateParameterInfoContext = MockCreateParameterInfoContext(file, myFixture)
            val parameterOwner = handler.findElementForParameterInfo(mockCreateParameterInfoContext) as PsiElement

            val textToType = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// TYPE:")
            if (textToType != null) {
                myFixture.type(textToType)
                PsiDocumentManager.getInstance(project).commitAllDocuments()
            }

            //to update current parameter index
            val updateContext = MockUpdateParameterInfoContext(file, myFixture, mockCreateParameterInfoContext)
            val elementForUpdating = handler.findElementForUpdatingParameterInfo(updateContext)
            if (elementForUpdating != null) {
                handler.updateParameterInfo(elementForUpdating, updateContext)
            }

            val parameterInfoUIContext = MockParameterInfoUIContext(parameterOwner, updateContext.currentParameter)

            mockCreateParameterInfoContext.itemsToShow?.forEach {
                handler.updateUI(it, parameterInfoUIContext)
            }

            Assert.assertEquals(expectedResultText, parameterInfoUIContext.resultText)
        }
    }
}
