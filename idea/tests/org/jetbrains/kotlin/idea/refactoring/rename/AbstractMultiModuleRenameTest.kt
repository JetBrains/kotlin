/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleRenameTest : KotlinMultiFileTestCase() {
    override fun getTestRoot(): String = "/refactoring/renameMultiModule/"
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase()

    fun doTest(path: String) {
        val renameParamsObject = loadTestConfiguration(File(path))

        val file = renameParamsObject.getString("file")
        val newName = renameParamsObject.getString("newName")

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            val mainFile = rootDir.findFileByRelativePath(file)!!
            val psiFile = PsiManager.getInstance(project).findFile(mainFile)!!

            val renameType = renameParamsObject.getString("type")

            when (RenameType.valueOf(renameType)) {
                RenameType.FILE -> runRenameProcessor(project, newName, psiFile, renameParamsObject, true, true)
                RenameType.MARKED_ELEMENT -> doRenameMarkedElement(renameParamsObject, psiFile)
                else -> TestCase.fail("Unexpected rename type: $renameType")
            }
        }
    }
}
