/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassToInnerProcessor
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.refactoring.move.moveInner.MoveInnerProcessor
import com.intellij.refactoring.move.moveMembers.MockMoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.util.ActionRunner
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.JetFileKotlinMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.MoveDestinationKotlinMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsOptions
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsProcessor
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public abstract class AbstractJetMoveTest : KotlinMultiFileTestCase() {
    protected fun doTest(path: String) {
        fun extractCaretOffset(doc: Document): Int {
            return runWriteAction {
                val text = StringBuilder(doc.getText())
                val offset = text.indexOf("<caret>")

                if (offset >= 0) {
                    text.delete(offset, offset + "<caret>".length())
                    doc.setText(text.toString())
                }

                offset
            }
        }

        val config = JsonParser().parse(FileUtil.loadFile(File(path), true)) as JsonObject


        val action = MoveAction.valueOf(config.getString("type"))

        val testDir = path.substring(0, path.lastIndexOf("/"))
        val mainFilePath = config.getNullableString("mainFile")!!

        val conflictFile = File(testDir + "/conflicts.txt")

        val withRuntime = config["withRuntime"]?.getAsBoolean() ?: false
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        doTest({ rootDir, rootAfter ->
            val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
            val mainPsiFile = PsiManager.getInstance(getProject()!!).findFile(mainFile)!!
            val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
            val editor = EditorFactory.getInstance()!!.createEditor(document, getProject()!!)!!

            val caretOffset = extractCaretOffset(document)
            val elementAtCaret = if (caretOffset >= 0) {
                TargetElementUtilBase.getInstance()!!.findTargetElement(
                        editor,
                        TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtilBase.ELEMENT_NAME_ACCEPTED,
                        caretOffset
                )
            }
            else null

            try {
                action.runRefactoring(rootDir, mainPsiFile, elementAtCaret, config)

                assert(!conflictFile.exists())
            }
            catch(e: ConflictsInTestsException) {
                JetTestUtils.assertEqualsToFile(conflictFile, e.getMessages().sort().joinToString("\n"))
            }
            finally {
                PsiDocumentManager.getInstance(getProject()!!).commitAllDocuments()
                FileDocumentManager.getInstance().saveAllDocuments()

                EditorFactory.getInstance()!!.releaseEditor(editor)

                if (withRuntime) {
                    ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
                }
            }
        },
        getTestDirName(true))
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.lastIndexOf('_')).replace('_', '/')
    }

    protected override fun getTestRoot() : String {
        return "/refactoring/move/"
    }

    protected override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}

enum class MoveAction {
    MOVE_MEMBERS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val member = elementAtCaret!!.getNonStrictParentOfType<PsiMember>()!!
            val targetClassName = config.getString("targetClass")
            val visibility = config.getNullableString("visibility")

            val options = MockMoveMembersOptions(targetClassName, array(member))
            if (visibility != null) {
                options.setMemberVisibility(visibility)
            }

            MoveMembersProcessor(elementAtCaret.getProject(), options).run()
        }
    }

    MOVE_TOP_LEVEL_CLASSES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val classToMove = elementAtCaret!!.getNonStrictParentOfType<PsiClass>()!!
            val targetPackage = config.getString("targetPackage")

            MoveClassesOrPackagesProcessor(
                    mainFile.getProject(),
                    array(classToMove),
                    MultipleRootsMoveDestination(PackageWrapper(mainFile.getManager(), targetPackage)),
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    }

    MOVE_PACKAGES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val project = mainFile.getProject()
            val sourcePackage = config.getString("sourcePackage")
            val targetPackage = config.getString("targetPackage")

            MoveClassesOrPackagesProcessor(
                    project,
                    array(JavaPsiFacade.getInstance(project).findPackage(sourcePackage)!!),
                    MultipleRootsMoveDestination(PackageWrapper(mainFile.getManager(), targetPackage)),
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    }

    MOVE_TOP_LEVEL_CLASSES_TO_INNER {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val project = mainFile.getProject()

            val classToMove = elementAtCaret!!.getNonStrictParentOfType<PsiClass>()!!
            val targetClass = config.getString("targetClass")

            MoveClassToInnerProcessor(
                    project,
                    array(classToMove),
                    JavaPsiFacade.getInstance(project).findClass(targetClass, project.allScope())!!,
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    }

    MOVE_INNER_CLASS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val project = mainFile.getProject()

            val classToMove = elementAtCaret!!.getNonStrictParentOfType<PsiClass>()!!
            val newClassName = config.getNullableString("newClassName") ?: classToMove.getName()!!
            val outerInstanceParameterName = config.getNullableString("outerInstanceParameterName")
            val targetPackage = config.getString("targetPackage")

            MoveInnerProcessor(
                    project,
                    classToMove,
                    newClassName,
                    outerInstanceParameterName != null,
                    outerInstanceParameterName,
                    JavaPsiFacade.getInstance(project).findPackage(targetPackage)!!.getDirectories()[0]
            ).run()
        }
    }

    MOVE_FILES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val project = mainFile.getProject()

            val targetPackage = config.getNullableString("targetPackage")
            if (targetPackage != null) {
                ActionRunner.runInsideWriteAction { VfsUtil.createDirectoryIfMissing(rootDir, targetPackage.replace('.', '/')) }
                MoveFilesOrDirectoriesProcessor(
                        project,
                        array(mainFile),
                        JavaPsiFacade.getInstance(project).findPackage(targetPackage)!!.getDirectories()[0],
                        /* searchInComments = */ false,
                        /* searchInNonJavaFiles = */ true,
                        /* moveCallback = */ null,
                        /* prepareSuccessfulCallback = */ null
                ).run()
            }
            else {
                val targetFile = config.getString("targetFile")

                MoveHandler.doMove(
                        project,
                        array(mainFile),
                        PsiManager.getInstance(project).findFile(rootDir.findFileByRelativePath(targetFile)!!)!!,
                        /* dataContext = */ null,
                        /* callback = */ null
                )
            }
        }
    }

    MOVE_KOTLIN_TOP_LEVEL_DECLARATIONS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject) {
            val project = mainFile.getProject()
            val elementToMove = elementAtCaret!!.getNonStrictParentOfType<JetNamedDeclaration>()!!

            val moveTarget = config.getNullableString("targetPackage")?.let { packageName ->
                MoveDestinationKotlinMoveTarget(MultipleRootsMoveDestination(PackageWrapper(mainFile.getManager(), packageName)))
            } ?: config.getString("targetFile").let { filePath ->
                JetFileKotlinMoveTarget(PsiManager.getInstance(project).findFile(rootDir.findFileByRelativePath(filePath)!!) as JetFile)
            }

            val options = MoveKotlinTopLevelDeclarationsOptions(listOf(elementToMove), moveTarget)
            MoveKotlinTopLevelDeclarationsProcessor(mainFile.getProject(), options).run()
        }
    }

    abstract fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject)
}
