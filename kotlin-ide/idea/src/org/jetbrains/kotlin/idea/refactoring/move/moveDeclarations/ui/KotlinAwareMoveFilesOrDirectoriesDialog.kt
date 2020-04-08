/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandler
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.ui.RecentsManager
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBLabelDecorator
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.packageMatchesDirectoryOrImplicit
import org.jetbrains.kotlin.idea.core.util.onTextChange
import org.jetbrains.kotlin.idea.refactoring.isInKotlinAwareSourceRoot
import org.jetbrains.kotlin.idea.refactoring.move.logFusForMoveRefactoring
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.JComponent

class KotlinAwareMoveFilesOrDirectoriesDialog(
    private val project: Project,
    private val initialDirectory: PsiDirectory?,
    private val psiElements: List<PsiFileSystemItem>,
    private val callback: MoveCallback?
) : DialogWrapper(project, true) {

    companion object {
        private const val RECENT_KEYS = "MoveFile.RECENT_KEYS"
        private const val MOVE_FILES_OPEN_IN_EDITOR = "MoveFile.OpenInEditor"
        private const val MOVE_FILES_SEARCH_REFERENCES = "MoveFile.SearchReferences"
        private const val HELP_ID = "refactoring.moveFile"

        private fun setConfigurationValue(id: String, value: Boolean, defaultValue: Boolean) {
            if (!ApplicationManager.getApplication().isUnitTestMode) {
                PropertiesComponent.getInstance().setValue(id, value, defaultValue)
            }
        }

        private fun getConfigurationValue(id: String, defaultValue: Boolean) =
            !ApplicationManager.getApplication().isUnitTestMode && PropertiesComponent.getInstance().getBoolean(id, defaultValue)
    }

    private data class CheckboxesState(
        val searchReferences: Boolean,
        val openInEditor: Boolean,
        val updatePackageDirective: Boolean
    )

    private val nameLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true)
    private val targetDirectoryField = TextFieldWithHistoryWithBrowseButton()
    private val searchReferencesCb = NonFocusableCheckBox(KotlinBundle.message("checkbox.text.search.references")).apply {
        isSelected = true
    }
    private val openInEditorCb = NonFocusableCheckBox(KotlinBundle.message("checkbox.text.open.moved.files.in.editor"))
    private val updatePackageDirectiveCb = NonFocusableCheckBox()
    private val initializedCheckBoxesState: CheckboxesState

    override fun getHelpId() = HELP_ID

    init {
        title = RefactoringBundle.message("move.title")
        init()
        initializeData()

        initializedCheckBoxesState = getCheckboxesState(applyDefaults = true)
    }

    private fun getCheckboxesState(applyDefaults: Boolean) = CheckboxesState(
        searchReferences = applyDefaults || searchReferencesCb.isSelected, //searchReferencesCb is true by default
        openInEditor = !applyDefaults && openInEditorCb.isSelected, //openInEditorCb is false by default
        updatePackageDirective = updatePackageDirectiveCb.isSelected
    )

    override fun createActions() = arrayOf(okAction, cancelAction, helpAction)

    override fun getPreferredFocusedComponent() = targetDirectoryField.childComponent

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel(): JComponent {
        RecentsManager.getInstance(project).getRecentEntries(RECENT_KEYS)?.let { targetDirectoryField.childComponent.history = it }

        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        targetDirectoryField.addBrowseFolderListener(
            RefactoringBundle.message("select.target.directory"),
            RefactoringBundle.message("the.file.will.be.moved.to.this.directory"),
            project,
            descriptor,
            TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
        )
        val textField = targetDirectoryField.childComponent.textEditor
        FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, disposable)
        textField.onTextChange { validateOKButton() }
        targetDirectoryField.setTextFieldPreferredWidth(CopyFilesOrDirectoriesDialog.MAX_PATH_LENGTH)
        Disposer.register(disposable, targetDirectoryField)

        openInEditorCb.isSelected = getConfigurationValue(id = MOVE_FILES_OPEN_IN_EDITOR, defaultValue = false)
        searchReferencesCb.isSelected = getConfigurationValue(id = MOVE_FILES_SEARCH_REFERENCES, defaultValue = true)

        val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION))
        return FormBuilder.createFormBuilder()
            .addComponent(nameLabel)
            .addLabeledComponent(RefactoringBundle.message("move.files.to.directory.label"), targetDirectoryField, UIUtil.LARGE_VGAP)
            .addTooltip(RefactoringBundle.message("path.completion.shortcut", shortcutText))
            .addComponentToRightColumn(searchReferencesCb, UIUtil.LARGE_VGAP)
            .addComponentToRightColumn(openInEditorCb, UIUtil.LARGE_VGAP)
            .addComponentToRightColumn(updatePackageDirectiveCb, UIUtil.LARGE_VGAP)
            .panel
    }

    private fun initializeData() {
        val psiElement = psiElements.singleOrNull()
        if (psiElement != null) {
            val shortenedPath = CopyFilesOrDirectoriesDialog.shortenPath(psiElement.virtualFile)
            nameLabel.text = when (psiElement) {
                is PsiFile -> RefactoringBundle.message("move.file.0", shortenedPath)
                else -> RefactoringBundle.message("move.directory.0", shortenedPath)
            }
        } else {
            val isFile = psiElements.all { it is PsiFile }
            val isDirectory = psiElements.all { it is PsiDirectory }
            nameLabel.text = when {
                isFile -> RefactoringBundle.message("move.specified.files")
                isDirectory -> RefactoringBundle.message("move.specified.directories")
                else -> RefactoringBundle.message("move.specified.elements")
            }
        }

        targetDirectoryField.childComponent.text = initialDirectory?.virtualFile?.presentableUrl ?: ""

        validateOKButton()

        with(updatePackageDirectiveCb) {
            val jetFiles = psiElements.filterIsInstance<KtFile>().filter(KtFile::isInKotlinAwareSourceRoot)

            if (jetFiles.isEmpty()) {
                parent.remove(updatePackageDirectiveCb)
                return
            }

            val singleFile = jetFiles.singleOrNull()
            isSelected = singleFile == null || singleFile.packageMatchesDirectoryOrImplicit()
            text = KotlinBundle.message("checkbox.text.update.package.directive")
        }
    }

    private fun validateOKButton() {
        isOKActionEnabled = targetDirectoryField.childComponent.text.isNotEmpty()
    }

    private val selectedDirectoryName: String
        get() = targetDirectoryField.childComponent.text.let {
            when {
                it.startsWith(".") -> (initialDirectory?.virtualFile?.path ?: "") + "/" + it
                else -> it
            }
        }

    private fun getModel(): Model {

        val directory = LocalFileSystem.getInstance().findFileByPath(selectedDirectoryName)?.let {
            PsiManager.getInstance(project).findDirectory(it)
        }

        val elementsToMove = directory?.let { existentDirectory ->
            val choice = if (psiElements.size > 1 || psiElements[0] is PsiDirectory) intArrayOf(-1) else null
            psiElements.filterNot {
                it is PsiFile && CopyFilesOrDirectoriesHandler.checkFileExist(existentDirectory, choice, it, it.name, title)
            }
        } ?: psiElements.toList()

        return KotlinAwareMoveFilesOrDirectoriesModel(
            project,
            elementsToMove,
            selectedDirectoryName,
            updatePackageDirectiveCb.isSelected,
            searchReferencesCb.isSelected,
            callback
        )
    }

    override fun doOKAction() {

        val modelResult: ModelResultWithFUSData
        try {
            modelResult = getModel().computeModelResult()
        } catch (e: ConfigurationException) {
            setErrorText(e.message)
            return
        }

        project.executeCommand(MoveHandler.REFACTORING_NAME) {
            with(modelResult) {
                logFusForMoveRefactoring(
                    numberOfEntities = elementsCount,
                    entity = entityToMove,
                    destination = destination,
                    isDefault = getCheckboxesState(applyDefaults = false) == initializedCheckBoxesState,
                    body = processor
                )
            }
        }

        setConfigurationValue(id = MOVE_FILES_SEARCH_REFERENCES, value = searchReferencesCb.isSelected, defaultValue = true)
        setConfigurationValue(id = MOVE_FILES_OPEN_IN_EDITOR, value = openInEditorCb.isSelected, defaultValue = false)

        RecentsManager.getInstance(project).registerRecentEntry(RECENT_KEYS, targetDirectoryField.childComponent.text)

        close(OK_EXIT_CODE, /* isOk = */ true)
    }
}