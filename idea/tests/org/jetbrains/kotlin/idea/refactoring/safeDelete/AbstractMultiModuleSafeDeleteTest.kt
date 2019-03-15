/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.google.gson.JsonObject
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.AbstractMultifileRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.refactoring.runRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.KotlinSafeDeleteProcessor.Companion.ALLOW_LIFTING_ACTUAL_PARAMETER_TO_EXPECTED
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.io.File

abstract class AbstractMultiModuleSafeDeleteTest : KotlinMultiFileTestCase() {
    object SafeDeleteAction : AbstractMultifileRefactoringTest.RefactoringAction {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            @Suppress("UNCHECKED_CAST")
            val elementClass = Class.forName(config.getString("elementClass")) as Class<PsiElement>
            val element = elementsAtCaret.single().getNonStrictParentOfType(elementClass)!!
            val project = mainFile.project
            project.ALLOW_LIFTING_ACTUAL_PARAMETER_TO_EXPECTED = config.get("liftParameterToExpected")?.asBoolean ?: true
            SafeDeleteHandler.invoke(project, arrayOf(element), null, true, null)
        }
    }

    override fun getTestRoot(): String = "/refactoring/safeDeleteMultiModule/"

    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase()

    fun doTest(path: String) {
        val config = loadTestConfiguration(File(path))

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            runRefactoringTest(path, config, rootDir, project, SafeDeleteAction)
        }
    }
}