/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.validateElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.properties.Delegates

class KotlinChangePropertySignatureDialog(
    project: Project,
    private val methodDescriptor: KotlinMethodDescriptor,
    private val commandName: String?
) : RefactoringDialog(project, true) {
    private val visibilityCombo = ComboBox(
        arrayOf(Visibilities.INTERNAL, Visibilities.PRIVATE, Visibilities.PROTECTED, Visibilities.PUBLIC)
    )

    private val nameField = EditorTextField(methodDescriptor.name)
    private var returnTypeField: EditorTextField by Delegates.notNull()
    private var receiverTypeCheckBox: JCheckBox? = null
    private var receiverTypeLabel: JLabel by Delegates.notNull()
    private var receiverTypeField: EditorTextField by Delegates.notNull()
    private var receiverDefaultValueLabel: JLabel? = null
    private var receiverDefaultValueField: EditorTextField? = null

    init {
        title = KotlinBundle.message("title.change.signature")
        init()
    }

    override fun getPreferredFocusedComponent() = nameField

    override fun createCenterPanel(): JComponent? {
        fun updateReceiverUI() {
            val withReceiver = receiverTypeCheckBox!!.isSelected
            receiverTypeLabel.isEnabled = withReceiver
            receiverTypeField.isEnabled = withReceiver
            receiverDefaultValueLabel?.isEnabled = withReceiver
            receiverDefaultValueField?.isEnabled = withReceiver
        }

        val documentManager = PsiDocumentManager.getInstance(myProject)
        val psiFactory = KtPsiFactory(myProject)

        return with(FormBuilder.createFormBuilder()) {
            val baseDeclaration = methodDescriptor.baseDeclaration
            if ((baseDeclaration as? KtProperty)?.isLocal != true) {
                visibilityCombo.selectedItem = methodDescriptor.visibility
                addLabeledComponent(KotlinBundle.message("label.text.visibility"), visibilityCombo)
            }

            addLabeledComponent(KotlinBundle.message("label.text.name"), nameField)

            val returnTypeCodeFragment = psiFactory.createTypeCodeFragment(
                methodDescriptor.returnTypeInfo.render(),
                baseDeclaration
            )
            returnTypeField = EditorTextField(documentManager.getDocument(returnTypeCodeFragment), myProject, KotlinFileType.INSTANCE)
            addLabeledComponent(KotlinBundle.message("label.text.type"), returnTypeField)

            if (baseDeclaration is KtProperty) {
                addSeparator()

                val receiverTypeCheckBox = JCheckBox(KotlinBundle.message("checkbox.text.extension.property"))
                receiverTypeCheckBox.addActionListener { updateReceiverUI() }
                receiverTypeCheckBox.isSelected = methodDescriptor.receiver != null
                addComponent(receiverTypeCheckBox)
                this@KotlinChangePropertySignatureDialog.receiverTypeCheckBox = receiverTypeCheckBox

                val receiverTypeCodeFragment = psiFactory.createTypeCodeFragment(
                    methodDescriptor.receiverTypeInfo.render(),
                    methodDescriptor.baseDeclaration
                )
                receiverTypeField =
                    EditorTextField(documentManager.getDocument(receiverTypeCodeFragment), myProject, KotlinFileType.INSTANCE)
                receiverTypeLabel = JLabel(KotlinBundle.message("label.text.receiver.type"))
                addLabeledComponent(receiverTypeLabel, receiverTypeField)

                if (methodDescriptor.receiver == null) {
                    val receiverDefaultValueCodeFragment = psiFactory.createExpressionCodeFragment("", methodDescriptor.baseDeclaration)
                    receiverDefaultValueField = EditorTextField(
                        documentManager.getDocument(receiverDefaultValueCodeFragment),
                        myProject,
                        KotlinFileType.INSTANCE
                    )
                    receiverDefaultValueLabel = JLabel(KotlinBundle.message("label.text.default.receiver.value"))
                    addLabeledComponent(receiverDefaultValueLabel, receiverDefaultValueField!!)
                }

                updateReceiverUI()
            }

            panel
        }
    }

    private fun getDefaultReceiverValue(): KtExpression? {
        val receiverDefaultValue = receiverDefaultValueField?.text ?: ""
        return if (receiverDefaultValue.isNotEmpty()) KtPsiFactory(myProject).createExpression(receiverDefaultValue) else null
    }

    override fun canRun() {
        val psiFactory = KtPsiFactory(myProject)

        psiFactory.createSimpleName(nameField.text).validateElement(
            KotlinBundle.message("error.text.invalid.name"))
        psiFactory.createType(returnTypeField.text).validateElement(
            KotlinBundle.message("error.text.invalid.return.type"))
        if (receiverTypeCheckBox?.isSelected == true) {
            psiFactory.createType(receiverTypeField.text).validateElement(
                KotlinBundle.message("error.text.invalid.receiver.type"))
        }
        getDefaultReceiverValue()?.validateElement(KotlinBundle.message("error.text.invalid.default.receiver.value"))
    }

    override fun doAction() {
        val originalDescriptor = methodDescriptor.original

        val receiver = if (receiverTypeCheckBox?.isSelected == true) {
            originalDescriptor.receiver ?: KotlinParameterInfo(
                callableDescriptor = originalDescriptor.baseDescriptor,
                name = "receiver",
                defaultValueForCall = getDefaultReceiverValue()
            )
        } else null
        receiver?.currentTypeInfo = KotlinTypeInfo(false, null, receiverTypeField.text)
        val changeInfo = KotlinChangeInfo(
            originalDescriptor,
            nameField.text,
            KotlinTypeInfo(true, null, returnTypeField.text),
            visibilityCombo.selectedItem as Visibility,
            emptyList(),
            receiver,
            originalDescriptor.method
        )

        invokeRefactoring(KotlinChangeSignatureProcessor(myProject, changeInfo, commandName ?: title))
    }

    companion object {
        fun createProcessorForSilentRefactoring(
            project: Project,
            commandName: String,
            descriptor: KotlinMethodDescriptor
        ): BaseRefactoringProcessor {
            val originalDescriptor = descriptor.original
            val changeInfo = KotlinChangeInfo(methodDescriptor = originalDescriptor, context = originalDescriptor.method)
            changeInfo.newName = descriptor.name
            changeInfo.receiverParameterInfo = descriptor.receiver
            return KotlinChangeSignatureProcessor(project, changeInfo, commandName)
        }
    }
}