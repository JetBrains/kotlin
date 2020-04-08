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

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.configuration.ExperimentalFeatures
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.isRunningInCidrIde
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.system.measureTimeMillis

var VirtualFile.pathBeforeJ2K: String? by UserDataProperty(Key.create<String>("PATH_BEFORE_J2K_CONVERSION"))

class JavaToKotlinAction : AnAction() {
    companion object {
        private fun uniqueKotlinFileName(javaFile: VirtualFile): String {
            val ioFile = File(javaFile.path.replace('/', File.separatorChar))

            var i = 0
            while (true) {
                val fileName = javaFile.nameWithoutExtension + (if (i > 0) i else "") + ".kt"
                if (!ioFile.resolveSibling(fileName).exists()) return fileName
                i++
            }
        }

        val title = KotlinBundle.message("action.j2k.name")

        private fun saveResults(javaFiles: List<PsiJavaFile>, convertedTexts: List<String>): List<VirtualFile> {
            val result = ArrayList<VirtualFile>()
            for ((psiFile, text) in javaFiles.zip(convertedTexts)) {
                try {
                    val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
                    val errorMessage = when {
                        document == null -> KotlinBundle.message("action.j2k.error.cant.find.document", psiFile.name)
                        !document.isWritable -> KotlinBundle.message("action.j2k.error.read.only", psiFile.name)
                        else -> null
                    }
                    if (errorMessage != null) {
                        val message = KotlinBundle.message("action.j2k.error.cant.save.result", errorMessage)
                        MessagesEx.error(psiFile.project, message).showLater()
                        continue
                    }
                    document!!.replaceString(0, document.textLength, text)
                    FileDocumentManager.getInstance().saveDocument(document)

                    val virtualFile = psiFile.virtualFile
                    if (ScratchRootType.getInstance().containsFile(virtualFile)) {
                        val mapping = ScratchFileService.getInstance().scratchesMapping
                        mapping.setMapping(virtualFile, KotlinFileType.INSTANCE.language)
                    } else {
                        val fileName = uniqueKotlinFileName(virtualFile)
                        virtualFile.pathBeforeJ2K = virtualFile.path
                        virtualFile.rename(this, fileName)
                    }
                    result += virtualFile
                } catch (e: IOException) {
                    MessagesEx.error(psiFile.project, e.message ?: "").showLater()
                }
            }
            return result
        }

        fun convertFiles(
            javaFiles: List<PsiJavaFile>,
            project: Project,
            module: Module,
            enableExternalCodeProcessing: Boolean = true,
            askExternalCodeProcessing: Boolean = true,
            forceUsingOldJ2k: Boolean = false
        ): List<KtFile> {
            var converterResult: FilesResult? = null
            fun convert() {
                val converter =
                    if (forceUsingOldJ2k) OldJavaToKotlinConverter(
                        project,
                        ConverterSettings.defaultSettings,
                        IdeaJavaToKotlinServices
                    ) else J2kConverterExtension.extension(useNewJ2k = ExperimentalFeatures.NewJ2k.isEnabled).createJavaToKotlinConverter(
                        project,
                        module,
                        ConverterSettings.defaultSettings,
                        IdeaJavaToKotlinServices
                    )
                converterResult = converter.filesToKotlin(
                    javaFiles,
                    if (forceUsingOldJ2k) J2kPostProcessor(formatCode = true)
                    else J2kConverterExtension.extension(useNewJ2k = ExperimentalFeatures.NewJ2k.isEnabled).createPostProcessor(formatCode = true),
                    progress = ProgressManager.getInstance().progressIndicator!!
                )
            }

            fun convertWithStatistics() {
                val conversionTime = measureTimeMillis {
                    convert()
                }
                val linesCount = runReadAction {
                    javaFiles.sumBy { StringUtil.getLineBreakCount(it.text) }
                }

                logJ2kConversionStatistics(
                    ConversionType.FILES,
                    ExperimentalFeatures.NewJ2k.isEnabled,
                    conversionTime,
                    linesCount,
                    javaFiles.size
                )
            }


            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ::convertWithStatistics,
                    title,
                    true,
                    project
                )
            ) return emptyList()


            var externalCodeUpdate: ((List<KtFile>) -> Unit)? = null

            val result = converterResult ?: return emptyList()
            val externalCodeProcessing = result.externalCodeProcessing
            if (enableExternalCodeProcessing && externalCodeProcessing != null) {
                val question = KotlinBundle.message("action.j2k.correction.required")
                if (!askExternalCodeProcessing || (Messages.showYesNoDialog(
                        project,
                        question,
                        title,
                        Messages.getQuestionIcon()
                    ) == Messages.YES)
                ) {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        {
                            runReadAction {
                                externalCodeUpdate = externalCodeProcessing.prepareWriteOperation(
                                    ProgressManager.getInstance().progressIndicator!!
                                )
                            }
                        },
                        title,
                        true,
                        project
                    )
                }
            }

            return project.executeWriteCommand(KotlinBundle.message("action.j2k.task.name"), null) {
                CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)

                val newFiles = saveResults(javaFiles, result.results)
                    .map { it.toPsiFile(project) as KtFile }
                    .onEach { it.commitAndUnblockDocument() }

                externalCodeUpdate?.invoke(newFiles)

                PsiDocumentManager.getInstance(project).commitAllDocuments()

                newFiles.singleOrNull()?.let {
                    FileEditorManager.getInstance(project).openFile(it.virtualFile, true)
                }

                newFiles
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val javaFiles = selectedJavaFiles(e).filter { it.isWritable }.toList()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val module = e.getData(LangDataKeys.MODULE) ?: return

        if (javaFiles.isEmpty()) {
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(KotlinBundle.message("action.j2k.errornothing.to.convert"), MessageType.ERROR, null)
                .createBalloon()
                .showInCenterOf(statusBar.component)
        }

        if (!J2kConverterExtension.extension(useNewJ2k = ExperimentalFeatures.NewJ2k.isEnabled).doCheckBeforeConversion(project, module)) return

        val firstSyntaxError = javaFiles.asSequence().map { PsiTreeUtil.findChildOfType(it, PsiErrorElement::class.java) }.firstOrNull()

        if (firstSyntaxError != null) {
            val count = javaFiles.filter { PsiTreeUtil.hasErrorElements(it) }.count()
            assert(count > 0)
            val firstFileName = firstSyntaxError.containingFile.name
            val question = when (count) {
                1 -> KotlinBundle.message("action.j2k.correction.errors.single", firstFileName)
                else -> KotlinBundle.message("action.j2k.correction.errors.multiple", firstFileName, count - 1)
            }
            val okText = KotlinBundle.message("action.j2k.correction.investigate")
            val cancelText = KotlinBundle.message("action.j2k.correction.proceed")
            if (Messages.showOkCancelDialog(
                    project,
                    question,
                    title,
                    okText,
                    cancelText,
                    Messages.getWarningIcon()
                ) == Messages.OK
            ) {
                NavigationUtil.activateFileWithPsiElement(firstSyntaxError.navigationElement)
                return
            }
        }

        convertFiles(javaFiles, project, module)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (isRunningInCidrIde) return false
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
        val project = e.project ?: return false
        e.getData(LangDataKeys.MODULE) ?: return false
        return isAnyJavaFileSelected(project, virtualFiles)
    }

    private fun isAnyJavaFileSelected(project: Project, files: Array<VirtualFile>): Boolean {
        if (files.any { it.isSuitableDirectory() }) return true // Giving up on directories
        val manager = PsiManager.getInstance(project)
        return files.any { it.extension == JavaFileType.DEFAULT_EXTENSION && manager.findFile(it) is PsiJavaFile && it.isWritable }
    }

    private fun VirtualFile.isSuitableDirectory(): Boolean =
        isDirectory && fileType !is ArchiveFileType && isWritable

    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = e.project ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
            .asSequence()
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }
}
