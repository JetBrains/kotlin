/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.refactoring.ui

import com.intellij.CommonBundle
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.daemon.impl.quickfix.ClassKind
import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Pass
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameHelper
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.refactoring.util.RefactoringMessageUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.RecentsManager
import com.intellij.ui.ReferenceEditorComboWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinBundle.message
import org.jetbrains.kotlin.idea.roots.getSuitableDestinationSourceRoots
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent

// Based on com.intellij.codeInsight.intention.impl.CreateClassDialog
open class CreateKotlinClassDialog(
    private val myProject: Project,
    title: String,
    private val myClassName: String,
    targetPackageName: String,
    kind: ClassKind,
    private val myClassNameEditable: Boolean,
    private val myModule: Module?,
    val isSealed: Boolean = false
) : DialogWrapper(myProject, true) {

    private val myInformationLabel = JLabel("#")
    private val myPackageLabel = JLabel(CodeInsightBundle.message("dialog.create.class.destination.package.label"))
    private var myPackageComponent: ReferenceEditorComboWithBrowseButton = PackageNameReferenceEditorCombo(
        targetPackageName,
        myProject,
        RECENTS_KEY,
        CodeInsightBundle.message("dialog.create.class.package.chooser.title")
    ).also {
        if (isSealed) it.isEnabled = false
    }
    private val myTfClassName: JTextField = MyTextField()
    var targetDirectory: PsiDirectory? = null
        private set
    private val myDestinationCB: KotlinDestinationFolderComboBox = object : KotlinDestinationFolderComboBox() {
        override fun getTargetPackage(): String {
            return myPackageComponent.text.trim()
        }

        override fun reportBaseInTestSelectionInSource(): Boolean {
            return this@CreateKotlinClassDialog.reportBaseInTestSelectionInSource()
        }

        override fun reportBaseInSourceSelectionInTest(): Boolean {
            return this@CreateKotlinClassDialog.reportBaseInSourceSelectionInTest()
        }
    }

    protected open fun reportBaseInTestSelectionInSource(): Boolean {
        return false
    }

    protected open fun reportBaseInSourceSelectionInTest(): Boolean {
        return false
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (myClassNameEditable) myTfClassName else myPackageComponent.childComponent
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel(BorderLayout())
    }

    override fun createNorthPanel(): JComponent? {
        val panel = JPanel(GridBagLayout())
        val gbConstraints = GridBagConstraints()
        gbConstraints.insets = JBUI.insets(4, 8)
        gbConstraints.fill = GridBagConstraints.HORIZONTAL
        gbConstraints.anchor = GridBagConstraints.WEST
        if (myClassNameEditable) {
            gbConstraints.weightx = 0.0
            gbConstraints.gridwidth = 1
            panel.add(myInformationLabel, gbConstraints)
            gbConstraints.insets = JBUI.insets(4, 8)
            gbConstraints.gridx = 1
            gbConstraints.weightx = 1.0
            gbConstraints.gridwidth = 1
            gbConstraints.fill = GridBagConstraints.HORIZONTAL
            gbConstraints.anchor = GridBagConstraints.WEST
            panel.add(myTfClassName, gbConstraints)
            myTfClassName.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    okAction.isEnabled = PsiNameHelper.getInstance(myProject).isIdentifier(myTfClassName.text)
                }
            })
            okAction.isEnabled = StringUtil.isNotEmpty(myClassName)
        }
        gbConstraints.gridx = 0
        gbConstraints.gridy = 2
        gbConstraints.weightx = 0.0
        gbConstraints.gridwidth = 1
        panel.add(myPackageLabel, gbConstraints)
        gbConstraints.gridx = 1
        gbConstraints.weightx = 1.0
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                myPackageComponent.button.doClick()
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
            myPackageComponent.childComponent
        )
        val _panel = JPanel(BorderLayout())
        _panel.add(myPackageComponent, BorderLayout.CENTER)
        panel.add(_panel, gbConstraints)
        gbConstraints.gridy = 3
        gbConstraints.gridx = 0
        gbConstraints.gridwidth = 2
        gbConstraints.insets.top = 12
        gbConstraints.anchor = GridBagConstraints.WEST
        gbConstraints.fill = GridBagConstraints.NONE
        val label = JBLabel(RefactoringBundle.message("target.destination.folder"))
        panel.add(label, gbConstraints)
        gbConstraints.gridy = 4
        gbConstraints.gridx = 0
        gbConstraints.fill = GridBagConstraints.HORIZONTAL
        gbConstraints.insets.top = 4
        panel.add(myDestinationCB, gbConstraints)
        val isMultipleSourceRoots = getSuitableDestinationSourceRoots(myProject).size > 1
        myDestinationCB.isVisible = isMultipleSourceRoots
        label.isVisible = isMultipleSourceRoots
        label.labelFor = myDestinationCB
        return panel
    }

    private val packageName: String
        get() {
            val name = myPackageComponent.text
            return name?.trim() ?: ""
        }

    private class MyTextField : JTextField() {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            val fontMetrics = getFontMetrics(font)
            size.width = fontMetrics.charWidth('a') * 40
            return size
        }
    }

    override fun doOKAction() {
        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, myPackageComponent.text)
        val packageName = packageName
        var errorString: String? = null
        CommandProcessor.getInstance().executeCommand(myProject, {
            try {
                val targetPackage = PackageWrapper(PsiManager.getInstance(myProject), packageName)
                val destination = myDestinationCB.selectDirectory(targetPackage, false) ?: return@executeCommand
                targetDirectory = WriteAction.compute<PsiDirectory, RuntimeException> {
                    val baseDir = getBaseDir(packageName)
                    if (baseDir == null && destination is MultipleRootsMoveDestination) {
                        errorString = message("destination.not.found.for.package.0", packageName)
                        return@compute null
                    }
                    destination.getTargetDirectory(baseDir)
                }
                if (targetDirectory == null) {
                    return@executeCommand
                }
                errorString = RefactoringMessageUtil.checkCanCreateClass(targetDirectory, className)
            } catch (e: IncorrectOperationException) {
                errorString = e.message
            }
        }, CodeInsightBundle.message("create.directory.command"), null)

        errorString?.let {
            if (it.isNotEmpty())
                Messages.showMessageDialog(myProject, errorString, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
        } ?: super.doOKAction()
    }

    protected open fun getBaseDir(packageName: String?): PsiDirectory? {
        return if (myModule == null) null else PackageUtil.findPossiblePackageDirectoryInModule(myModule, packageName)
    }

    val className: String
        get() = if (myClassNameEditable) {
            myTfClassName.text
        } else {
            myClassName
        }

    companion object {
        @NonNls
        private val RECENTS_KEY = "CreateKotlinClassDialog.RecentsKey"
    }

    init {
        myPackageComponent.setTextFieldPreferredWidth(40)
        init()
        if (!myClassNameEditable) {
            setTitle(CodeInsightBundle.message("dialog.create.class.name", StringUtil.capitalize(kind.description), myClassName))
        } else {
            myInformationLabel.text = CodeInsightBundle.message("dialog.create.class.label", kind.description)
            setTitle(title)
        }
        myTfClassName.text = myClassName
        myDestinationCB.setData(myProject, getBaseDir(targetPackageName), object : Pass<String?>() {
            override fun pass(s: String?) {
                setErrorText(s, myDestinationCB)
            }
        }, myPackageComponent.childComponent, isSealed)
    }
}