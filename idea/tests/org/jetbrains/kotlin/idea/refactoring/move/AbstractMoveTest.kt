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
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveClassesOrPackages.*
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.refactoring.move.moveInner.MoveInnerProcessor
import com.intellij.refactoring.move.moveMembers.MockMoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ActionRunner
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.changePackage.KotlinChangePackageRefactoring
import org.jetbrains.kotlin.idea.refactoring.move.moveClassesOrPackages.KotlinAwareDelegatingMoveDestination
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractMoveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.getProjectDescriptor()

        val testConfigurationFile = File(super.getTestDataPath(), fileName())
        val config = loadTestConfiguration(testConfigurationFile)
        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        if (withRuntime) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun doTest(path: String) {
        val testFile = File(path)
        val config = JsonParser().parse(FileUtil.loadFile(testFile, true)) as JsonObject

        doTestCommittingDocuments(testFile) { rootDir ->
            runMoveRefactoring(path, config, rootDir, project)
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        val endIndex = testName.lastIndexOf('_')
        if (endIndex < 0) return testName
        return testName.substring(0, endIndex).replace('_', '/')
    }

    override fun getTestDataPath() = super.getTestDataPath() + "/" + getTestDirName(true)

    protected fun doTestCommittingDocuments(testFile: File, action: (VirtualFile) -> Unit) {
        val beforeVFile = myFixture.copyDirectoryToProject("before", "")
        PsiDocumentManager.getInstance(myFixture.project).commitAllDocuments()

        val afterDir = File(testFile.parentFile, "after")
        val afterVFile = LocalFileSystem.getInstance().findFileByIoFile(afterDir)?.apply {
            UsefulTestCase.refreshRecursively(this)
        }

        action(beforeVFile)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
        PlatformTestUtil.assertDirectoriesEqual(afterVFile, beforeVFile)
    }
}

fun runMoveRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
    val action = MoveAction.valueOf(config.getString("type"))

    val testDir = path.substring(0, path.lastIndexOf("/"))
    val mainFilePath = config.getNullableString("mainFile") ?: config.getAsJsonArray("filesToMove").first().asString

    val conflictFile = File(testDir + "/conflicts.txt")

    val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
    val mainPsiFile = PsiManager.getInstance(project).findFile(mainFile)!!
    val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
    val editor = EditorFactory.getInstance()!!.createEditor(document, project)!!

    val caretOffsets = document.extractMultipleMarkerOffsets(project)
    val elementsAtCaret = caretOffsets.map {
        TargetElementUtil.getInstance().findTargetElement(
                editor,
                TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.ELEMENT_NAME_ACCEPTED,
                it
        )!!
    }

    try {
        action.runRefactoring(rootDir, mainPsiFile, elementsAtCaret, config)

        assert(!conflictFile.exists())
    }
    catch(e: ConflictsInTestsException) {
        KotlinTestUtils.assertEqualsToFile(conflictFile, e.messages.distinct().sorted().joinToString("\n"))

        // TODO: hack it with reflection, only for as 2.2
        //ConflictsInTestsException.setTestIgnore(true)

        // Run refactoring again with ConflictsInTestsException suppressed
        action.runRefactoring(rootDir, mainPsiFile, elementsAtCaret, config)
    }
    finally {
        // TODO: hack it with reflection, only for as 2.2
        //ConflictsInTestsException.setTestIgnore(false)

        EditorFactory.getInstance()!!.releaseEditor(editor)
    }
}

enum class MoveAction {
    MOVE_MEMBERS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val members = elementsAtCaret.map { it.getNonStrictParentOfType<PsiMember>()!! }
            val targetClassName = config.getString("targetClass")
            val visibility = config.getNullableString("visibility")

            val options = MockMoveMembersOptions(targetClassName, members.toTypedArray())
            if (visibility != null) {
                options.memberVisibility = visibility
            }

            MoveMembersProcessor(mainFile.project, options).run()
        }
    },

    MOVE_TOP_LEVEL_CLASSES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val classesToMove = elementsAtCaret.map { it.getNonStrictParentOfType<PsiClass>()!! }
            val targetPackage = config.getString("targetPackage")

            MoveClassesOrPackagesProcessor(
                    mainFile.project,
                    classesToMove.toTypedArray(),
                    MultipleRootsMoveDestination(PackageWrapper(mainFile.manager, targetPackage)),
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    },

    MOVE_PACKAGES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val sourcePackageName = config.getString("sourcePackage")
            val targetPackageName = config.getString("targetPackage")

            val sourcePackage = JavaPsiFacade.getInstance(project).findPackage(sourcePackageName)!!
            val targetPackage = JavaPsiFacade.getInstance(project).findPackage(targetPackageName)
            val targetDirectory = targetPackage?.directories?.first()

            val targetPackageWrapper = PackageWrapper(mainFile.manager, targetPackageName)
            val moveDestination = if (targetDirectory != null) {
                val targetSourceRoot = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(targetDirectory.virtualFile)!!
                KotlinAwareDelegatingMoveDestination(
                        AutocreatingSingleSourceRootMoveDestination(targetPackageWrapper, targetSourceRoot),
                        targetPackage,
                        targetDirectory
                )
            }
            else {
                MultipleRootsMoveDestination(targetPackageWrapper)
            }

            MoveClassesOrPackagesProcessor(
                    project,
                    arrayOf(sourcePackage),
                    moveDestination,
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    },

    MOVE_TOP_LEVEL_CLASSES_TO_INNER {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project

            val classesToMove = elementsAtCaret.map { it.getNonStrictParentOfType<PsiClass>()!! }
            val targetClass = config.getString("targetClass")

            MoveClassToInnerProcessor(
                    project,
                    classesToMove.toTypedArray(),
                    JavaPsiFacade.getInstance(project).findClass(targetClass, project.allScope())!!,
                    /* searchInComments = */ false,
                    /* searchInNonJavaFiles = */ true,
                    /* moveCallback = */ null
            ).run()
        }
    },

    MOVE_INNER_CLASS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project

            val classToMove = elementsAtCaret.single().getNonStrictParentOfType<PsiClass>()!!
            val newClassName = config.getNullableString("newClassName") ?: classToMove.name!!
            val outerInstanceParameterName = config.getNullableString("outerInstanceParameterName")
            val targetPackage = config.getString("targetPackage")

            MoveInnerProcessor(
                    project,
                    classToMove,
                    newClassName,
                    outerInstanceParameterName != null,
                    outerInstanceParameterName,
                    JavaPsiFacade.getInstance(project).findPackage(targetPackage)!!.directories[0]
            ).run()
        }
    },

    MOVE_FILES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project

            val targetPackage = config.getNullableString("targetPackage")
            val targetDirPath = targetPackage?.replace('.', '/') ?: config.getNullableString("targetDirectory")
            if (targetDirPath != null) {
                ActionRunner.runInsideWriteAction { VfsUtil.createDirectoryIfMissing(rootDir, targetDirPath) }
                val newParent = if (targetPackage != null) {
                    JavaPsiFacade.getInstance(project).findPackage(targetPackage)!!.directories[0]
                }
                else {
                    rootDir.findFileByRelativePath(targetDirPath)!!.toPsiDirectory(project)!!
                }
                MoveFilesOrDirectoriesProcessor(
                        project,
                        arrayOf(mainFile),
                        newParent,
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
                        arrayOf(mainFile),
                        PsiManager.getInstance(project).findFile(rootDir.findFileByRelativePath(targetFile)!!)!!,
                        /* dataContext = */ null,
                        /* callback = */ null
                )
            }
        }
    },

    MOVE_FILES_WITH_DECLARATIONS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val elementsToMove = config.getAsJsonArray("filesToMove").map {
                val virtualFile = rootDir.findFileByRelativePath(it.asString)!!
                if (virtualFile.isDirectory) virtualFile.toPsiDirectory(project)!! else virtualFile.toPsiFile(project)!!
            }
            val targetDirPath = config.getString("targetDirectory")
            val targetDir = rootDir.findFileByRelativePath(targetDirPath)!!.toPsiDirectory(project)!!
            KotlinAwareMoveFilesOrDirectoriesProcessor(
                    project,
                    elementsToMove,
                    targetDir,
                    searchInComments = true,
                    searchInNonJavaFiles = true,
                    moveCallback = null
            ).run()
        }
    },

    MOVE_KOTLIN_TOP_LEVEL_DECLARATIONS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val elementsToMove = elementsAtCaret.map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }

            val moveTarget = config.getNullableString("targetPackage")?.let { packageName ->
                val targetSourceRootPath = config["targetSourceRoot"]?.asString
                val packageWrapper = PackageWrapper(mainFile.manager, packageName)
                val moveDestination: MoveDestination = targetSourceRootPath?.let {
                    AutocreatingSingleSourceRootMoveDestination(packageWrapper, rootDir.findFileByRelativePath(it)!!)
                } ?: MultipleRootsMoveDestination(packageWrapper)
                val targetDir = moveDestination.getTargetIfExists(mainFile)
                val targetVirtualFile = if (targetSourceRootPath != null) {
                    rootDir.findFileByRelativePath(targetSourceRootPath)!!
                } else {
                    targetDir?.virtualFile
                }

                KotlinMoveTargetForDeferredFile(FqName(packageName), targetDir, targetVirtualFile) {
                    createKotlinFile(guessNewFileName(elementsToMove)!!, moveDestination.getTargetDirectory(mainFile))
                }
            } ?: config.getString("targetFile").let { filePath ->
                KotlinMoveTargetForExistingElement(PsiManager.getInstance(project).findFile(rootDir.findFileByRelativePath(filePath)!!) as KtFile)
            }

            val descriptor = MoveDeclarationsDescriptor(project, elementsToMove, moveTarget, MoveDeclarationsDelegate.TopLevel)
            MoveKotlinDeclarationsProcessor(descriptor).run()
        }
    },

    CHANGE_PACKAGE_DIRECTIVE {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            KotlinChangePackageRefactoring(mainFile as KtFile).run(FqName(config.getString("newPackageName")))
        }
    },

    MOVE_DIRECTORY_WITH_CLASSES {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val sourceDir = rootDir.findFileByRelativePath(config.getString("sourceDir"))!!.toPsiDirectory(project)!!
            val targetDir = rootDir.findFileByRelativePath(config.getString("targetDir"))!!.toPsiDirectory(project)!!
            MoveDirectoryWithClassesProcessor(project, arrayOf(sourceDir), targetDir, true, true, true, {}).run()
        }
    },

    MOVE_KOTLIN_NESTED_CLASS {
        override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
            val project = mainFile.project
            val elementToMove = elementsAtCaret.single().getNonStrictParentOfType<KtClassOrObject>()!!
            val targetClassName = config.getNullableString("targetClass")
            val targetClass =
                    if (targetClassName != null) {
                        KotlinFullClassNameIndex.getInstance().get(targetClassName, project, project.projectScope()).first()!!
                    }
                    else null
            val delegate = MoveDeclarationsDelegate.NestedClass(config.getNullableString("newName"),
                                                                config.getNullableString("outerInstanceParameter"))
            val moveTarget =
                    if (targetClass != null) {
                        KotlinMoveTargetForExistingElement(targetClass)
                    }
                    else {
                        val fileName = (delegate.newClassName ?: elementToMove.name!!) + ".kt"
                        val targetPackageFqName = (mainFile as KtFile).packageFqName
                        val targetDir = mainFile.containingDirectory!!
                        KotlinMoveTargetForDeferredFile(targetPackageFqName, targetDir, null) {
                            createKotlinFile(fileName, targetDir, targetPackageFqName.asString())
                        }
                    }
            val descriptor = MoveDeclarationsDescriptor(project, listOf(elementToMove), moveTarget, delegate)
            MoveKotlinDeclarationsProcessor(descriptor).run()
        }
    };

    abstract fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject)
}
