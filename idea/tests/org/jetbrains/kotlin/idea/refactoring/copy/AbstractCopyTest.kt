/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.copy

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.copy.CopyHandler
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.AbstractMultifileRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.copy.CopyKotlinDeclarationsHandler.Companion.newName
import org.jetbrains.kotlin.idea.refactoring.runRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.utils.ifEmpty

abstract class AbstractCopyTest : AbstractMultifileRefactoringTest() {
    companion object : RefactoringAction {
        fun runCopyRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
            runRefactoringTest(path, config, rootDir, project, this)
        }

        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project

            val elementsToCopy = elementsAtCaret.ifEmpty { listOf(mainFile) }.toTypedArray()
            assert(CopyHandler.canCopy(elementsToCopy))

            val targetDirectory = config.getNullableString("targetDirectory")?.let {
                rootDir.findFileByRelativePath(it)?.toPsiDirectory(project)
            }
            ?: run {
                val packageWrapper = PackageWrapper(mainFile.manager, config.getString("targetPackage"))
                runWriteAction { MultipleRootsMoveDestination(packageWrapper).getTargetDirectory(mainFile) }
            }

            project.newName = config.getNullableString("newName")

            CopyHandler.doCopy(elementsToCopy, targetDirectory)
        }
    }

    override fun runRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
        runCopyRefactoring(path, config, rootDir, project)
    }
}