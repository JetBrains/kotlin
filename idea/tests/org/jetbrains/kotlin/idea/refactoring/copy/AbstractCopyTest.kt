/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.AbstractMultifileRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.copy.CopyKotlinDeclarationsHandler.Companion.newName
import org.jetbrains.kotlin.idea.refactoring.runRefactoringTest
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.utils.ifEmpty

private enum class CopyAction : AbstractMultifileRefactoringTest.RefactoringAction {
    COPY_FILES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val elementsToCopy = config.getAsJsonArray("filesToMove").map {
                val virtualFile = rootDir.findFileByRelativePath(it.asString)!!
                if (virtualFile.isDirectory) virtualFile.toPsiDirectory(project)!! else virtualFile.toPsiFile(project)!!
            }

            DEFAULT.runRefactoring(rootDir, mainFile, elementsToCopy, config)
        }
    },

    DEFAULT {
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
}

abstract class AbstractCopyTest : AbstractMultifileRefactoringTest() {

    companion object {
        fun runCopyRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
            val action = config.getNullableString("type")?.let { CopyAction.valueOf(it) } ?: CopyAction.DEFAULT
            runRefactoringTest(path, config, rootDir, project, action)
        }
    }

    override fun runRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
        runCopyRefactoring(path, config, rootDir, project)
    }
}