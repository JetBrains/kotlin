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

package org.jetbrains.kotlin.idea.nodejs.protractor

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.javascript.protractor.ProtractorUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.ex.SingleConfigurableEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper

import javax.swing.*
import java.util.Collections

// Based com.intellij.javascript.protractor.ProtractorRunConfigurationEditor
class KotlinProtractorRunConfigurationEditor(private val myProject: Project) : SettingsEditor<KotlinProtractorRunConfiguration>() {
    private val testFilePathTextFieldWithBrowseButton: TextFieldWithHistoryWithBrowseButton
    private val seleniumAddressTextField: JTextField
    private val extraOptionsEditor: RawCommandLineEditor
    private val configPathTextFieldWithBrowseButton: TextFieldWithHistoryWithBrowseButton
    private val interpreterField: NodeJsInterpreterField
    private val protractorPackageField: NodePackageField
    private val envVarsComponent: EnvironmentVariablesTextFieldWithBrowseButton
    private val panel: JPanel

    init {
        testFilePathTextFieldWithBrowseButton = createTestFileTextField(myProject)
        seleniumAddressTextField = JTextField()
        extraOptionsEditor = RawCommandLineEditor().apply { dialogCaption = "Extra Protractor Options" }
        configPathTextFieldWithBrowseButton = createConfigurationFileTextField(myProject)
        interpreterField = NodeJsInterpreterField(myProject, false)
        protractorPackageField = NodePackageField(myProject, ProtractorUtil.PACKAGE_NAME) { interpreterField.interpreter }
        envVarsComponent = EnvironmentVariablesTextFieldWithBrowseButton()
        panel = FormBuilder()
                .setAlignLabelOnRight(false)
                .addLabeledComponent("&Test file:", testFilePathTextFieldWithBrowseButton)
                .addLabeledComponent("&Selenium address:", seleniumAddressTextField)
                .addLabeledComponent("E&xtra Protractor options:", extraOptionsEditor)
                .addComponent(JSeparator(), JBUI.scale(8))
                .addLabeledComponent("&Configuration file:", configPathTextFieldWithBrowseButton)
                .addComponent(JSeparator(), JBUI.scale(8))
                .addLabeledComponent("Node &interpreter:", interpreterField, JBUI.scale(8))
                .addLabeledComponent("&Protractor package:", protractorPackageField)
                .addLabeledComponent("&Environment variables:", envVarsComponent)
                .panel
    }

    private fun createConfigurationFileTextField(project: Project) = TextFieldWithHistoryWithBrowseButton().apply {
        val textFieldWithHistory = childComponent
        textFieldWithHistory.setHistorySize(-1)
        textFieldWithHistory.setMinimumAndPreferredWidth(0)
        SwingHelper.addHistoryOnExpansion(textFieldWithHistory) {
            val newFiles = ProtractorUtil.listPossibleConfigFilesInProject(project)
            val newFilePaths = ContainerUtil.map(newFiles) { file -> FileUtil.toSystemDependentName(file.path) }
            Collections.sort(newFilePaths)
            newFilePaths
        }

        SwingHelper.installFileCompletionAndBrowseDialog(
                project,
                this,
                "Select Protractor Configuration File",
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        )
    }

    private fun createTestFileTextField(project: Project) = TextFieldWithHistoryWithBrowseButton().apply {
        val textFieldWithHistory = childComponent
        textFieldWithHistory.setHistorySize(-1)
        textFieldWithHistory.setMinimumAndPreferredWidth(0)

        SwingHelper.installFileCompletionAndBrowseDialog(
                project,
                this,
                "Select Protractor Configuration File",
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        )
    }

    override fun resetEditorFrom(runConfiguration: KotlinProtractorRunConfiguration) {
        with(runConfiguration.runSettings) {
            testFilePathTextFieldWithBrowseButton.setTextAndAddToHistory(testFileSystemDependentPath)
            seleniumAddressTextField.text = seleniumAddress
            extraOptionsEditor.text = extraOptions
            configPathTextFieldWithBrowseButton.setTextAndAddToHistory(configFileSystemDependentPath)
            interpreterField.interpreterRef = interpreterRef
            protractorPackageField.selected = runConfiguration.protractorPackage
            envVarsComponent.data = envData
        }
        updatePreferredWidth()
    }

    private fun updatePreferredWidth() {
        val dialogWrapper = DialogWrapper.findInstance(panel)
        if (dialogWrapper is SingleConfigurableEditor) {
            // dialog for single run configuration
            interpreterField.setPreferredWidthToFitText()
            protractorPackageField.setPreferredWidthToFitText()
            SwingHelper.setPreferredWidthToFitText(testFilePathTextFieldWithBrowseButton)
            SwingHelper.setPreferredWidthToFitText(seleniumAddressTextField)
            SwingHelper.setPreferredWidthToFitText(configPathTextFieldWithBrowseButton)
            ApplicationManager.getApplication().invokeLater { SwingHelper.adjustDialogSizeToFitPreferredSize(dialogWrapper) }
        }
    }

    override fun applyEditorTo(runConfiguration: KotlinProtractorRunConfiguration) {
        ProtractorUtil.setProtractorPackage(myProject, protractorPackageField.selected)
        runConfiguration.runSettings = runConfiguration.runSettings.copy(
                interpreterRef = interpreterField.interpreterRef,
                configFilePath = configPathTextFieldWithBrowseButton.text,
                testFilePath = testFilePathTextFieldWithBrowseButton.text,
                seleniumAddress = seleniumAddressTextField.text,
                extraOptions = extraOptionsEditor.text,
                envData = envVarsComponent.data
        )
    }

    override fun createEditor() = panel
}
