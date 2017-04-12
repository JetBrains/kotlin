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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveClassesOrPackages.*
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.refactoring.move.moveInner.MoveInnerProcessor
import com.intellij.refactoring.move.moveMembers.MockMoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.util.ActionRunner
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.refactoring.move.changePackage.KotlinChangePackageRefactoring
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class AbstractMoveTest : AbstractMultifileRefactoringTest() {
    override fun runRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
        runMoveRefactoring(path, config, rootDir, project)
    }
}

fun runMoveRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
    runRefactoringTest(path, config, rootDir, project, MoveAction.valueOf(config.getString("type")))
}

enum class MoveAction : AbstractMultifileRefactoringTest.RefactoringAction {
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
            val sourcePackage = config.getString("sourcePackage")
            val targetPackage = config.getString("targetPackage")

            MoveClassesOrPackagesProcessor(
                    project,
                    arrayOf(JavaPsiFacade.getInstance(project).findPackage(sourcePackage)!!),
                    MultipleRootsMoveDestination(PackageWrapper(mainFile.manager, targetPackage)),
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
}
