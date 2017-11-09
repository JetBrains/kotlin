/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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