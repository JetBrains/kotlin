// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.wizard

import com.intellij.ide.IdeBundle
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.externalSystem.util.properties.PropertyGraph
import com.intellij.openapi.externalSystem.util.properties.ObservableClearableProperty
import com.intellij.openapi.externalSystem.util.properties.PropertyView.Companion.comap
import com.intellij.openapi.externalSystem.util.properties.PropertyView.Companion.map
import com.intellij.openapi.externalSystem.util.properties.UiPropertyImpl.Companion.uiProperty
import com.intellij.openapi.externalSystem.util.ui.DataView
import com.intellij.openapi.externalSystem.util.ui.bind
import com.intellij.openapi.externalSystem.util.ui.myComboBox
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleLocalFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.layout.*
import java.io.File
import java.util.Comparator.comparing
import java.util.function.Function
import javax.swing.JList
import javax.swing.JTextField
import javax.swing.ListCellRenderer

abstract class MavenizedStructureWizardStep<Data : Any>(val context: WizardContext) : ModuleWizardStep() {

  abstract fun createView(data: Data): DataView<Data>

  abstract fun findAllParents(): List<Data>

  private val propertyGraph = PropertyGraph()
  private val entityNameProperty = propertyGraph.uiProperty(::suggestName)
  private val locationProperty = propertyGraph.uiProperty { getUiPath(suggestLocationByName()) }
  private val parentProperty = propertyGraph.uiProperty(::suggestParentByLocation)
  private val groupIdProperty = propertyGraph.uiProperty(::suggestGroupIdByParent)
  private val artifactIdProperty = propertyGraph.uiProperty(::suggestArtifactIdByName)
  private val versionProperty = propertyGraph.uiProperty(::suggestVersionByParent)

  var entityName by entityNameProperty.map { it.trim() }
  var location by locationProperty.map { getModelPath(it) }.comap { getUiPath(it) }
  var parent by parentProperty
  var groupId by groupIdProperty.map { it.trim() }
  var artifactId by artifactIdProperty.map { it.trim() }
  var version by versionProperty.map { it.trim() }

  val parents by lazy { parentsData.map(::createView) }
  val parentsData by lazy { findAllParents() }
  var parentData: Data?
    get() = DataView.getData(parent)
    set(value) {
      parent = if (value == null) EMPTY_VIEW else createView(value)
    }

  init {
    entityNameProperty.dependsOn(locationProperty, ::suggestNameByLocation)
    entityNameProperty.dependsOn(artifactIdProperty, ::suggestNameByArtifactId)
    parentProperty.dependsOn(locationProperty, ::suggestParentByLocation)
    locationProperty.dependsOn(parentProperty) { getUiPath(suggestLocationByParentAndName()) }
    locationProperty.dependsOn(entityNameProperty) { getUiPath(suggestLocationByParentAndName()) }
    groupIdProperty.dependsOn(parentProperty, ::suggestGroupIdByParent)
    artifactIdProperty.dependsOn(entityNameProperty, ::suggestArtifactIdByName)
    versionProperty.dependsOn(parentProperty, ::suggestVersionByParent)
  }

  private val contentPanel by lazy {
    panel {
      if (!context.isCreatingNewProject) {
        row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.parent.label")) {
          val presentationName = Function<DataView<Data>, String> { it.presentationName }
          val parentComboBoxModel = SortedComboBoxModel(comparing(presentationName, String.CASE_INSENSITIVE_ORDER))
          parentComboBoxModel.add(EMPTY_VIEW)
          parentComboBoxModel.addAll(parents)
          myComboBox(parentComboBoxModel, parentProperty.asBinding(), getParentRenderer()).apply {
            component.bind(parentProperty, ::validateParent, context.disposable)
          }
        }
      }
      row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.name.label")) {
        textField(entityNameProperty.asBinding()).focused().apply {
          component.bind(entityNameProperty, ::validateName, context.disposable)
        }
      }
      row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.location.label")) {
        val fileChooserDescriptor = createSingleLocalFileDescriptor().withFileFilter { it.isDirectory }
        val fileChosen = { file: VirtualFile -> getUiPath(file.path) }
        val title = IdeBundle.message("title.select.project.file.directory", context.presentationName)
        textFieldWithBrowseButton(locationProperty.asBinding(), title, context.project, fileChooserDescriptor, fileChosen).apply {
          component.bind(locationProperty, ::validateLocation, context.disposable)
        }
      }
      hideableRow(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.coordinates.title")) {
        row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.group.id.label")) {
          textField(groupIdProperty.asBinding()).apply {
            comment(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.group.id.help"))
            component.bind(groupIdProperty, ::validateGroupId, context.disposable)
          }
        }
        row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.id.label")) {
          textField(artifactIdProperty.asBinding()).apply {
            comment(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.id.help", context.presentationName))
            component.bind(artifactIdProperty, ::validateArtifactId, context.disposable)
          }
        }
        row(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.version.label")) {
          textField(versionProperty.asBinding()).apply {
            component.bind(versionProperty, ::validateVersion, context.disposable)
          }
        }
      }
    }
  }

  override fun getPreferredFocusedComponent() = contentPanel.preferredFocusedComponent

  override fun getComponent() = contentPanel

  override fun updateStep() = (preferredFocusedComponent as JTextField).selectAll()

  private fun getParentRenderer(): ListCellRenderer<DataView<Data>> {
    return object : SimpleListCellRenderer<DataView<Data>>() {
      override fun customize(list: JList<out DataView<Data>>,
                             value: DataView<Data>,
                             index: Int,
                             selected: Boolean,
                             hasFocus: Boolean) {
        text = value.presentationName
        icon = DataView.getIcon(value)
      }
    }
  }

  private fun getUiPath(path: String): String = getLocationRelativeToUserHome(toSystemDependentName(path.trim()), false)

  private fun getModelPath(path: String): String = toCanonicalPath(expandUserHome(path.trim()))

  protected open fun suggestName(): String {
    val projectFileDirectory = File(context.projectFileDirectory)
    return createSequentFileName(projectFileDirectory, "untitled", "")
  }

  protected open fun suggestNameByLocation(): String {
    return File(location).name
  }

  protected open fun suggestNameByArtifactId(): String {
    return artifactId
  }

  protected open fun suggestLocationByParentAndName(): String {
    if (!parent.isPresent) return suggestLocationByName()
    return join(parent.location, entityName)
  }

  protected open fun suggestLocationByName(): String {
    return join(context.projectFileDirectory, entityName)
  }

  protected open fun suggestParentByLocation(): DataView<Data> {
    val location = location
    return parents.find { isAncestor(it.location, location, true) } ?: EMPTY_VIEW
  }

  protected open fun suggestGroupIdByParent(): String {
    return parent.groupId
  }

  protected open fun suggestArtifactIdByName(): String {
    return entityName
  }

  protected open fun suggestVersionByParent(): String {
    return parent.version
  }

  override fun validate(): Boolean {
    val validationResults = listOf(
      parentProperty.validate(),
      entityNameProperty.validate(),
      locationProperty.validate(),
      groupIdProperty.validate(),
      artifactIdProperty.validate(),
      versionProperty.validate()
    )
    return validationResults.filterNotNull().all { it.okEnabled }
  }

  protected open fun validateGroupId(): ValidationInfo? {
    if (groupId.isEmpty()) {
      val propertyPresentation = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.group.id.presentation")
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.missing.error",
                                                 context.presentationName, propertyPresentation)
      return ValidationInfo(message)
    }
    return null
  }

  protected open fun validateArtifactId(): ValidationInfo? {
    if (artifactId.isEmpty()) {
      val propertyPresentation = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.id.presentation")
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.missing.error",
                                                 context.presentationName, propertyPresentation)
      return ValidationInfo(message)
    }
    return null
  }

  protected open fun validateVersion(): ValidationInfo? {
    if (version.isEmpty()) {
      val propertyPresentation = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.version.presentation")
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.missing.error",
                                                 context.presentationName, propertyPresentation)
      return ValidationInfo(message)
    }
    return null
  }

  protected open fun validateName(): ValidationInfo? {
    if (entityName.isEmpty()) {
      val propertyPresentation = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.name.presentation")
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.missing.error",
                                                 context.presentationName, propertyPresentation)
      return ValidationInfo(message)
    }
    return null
  }

  protected open fun validateLocation(): ValidationInfo? {
    val location = location
    if (location.isEmpty()) {
      val propertyPresentation = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.location.presentation")
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.missing.error",
                                                 context.presentationName, propertyPresentation)
      return ValidationInfo(message)
    }

    for (project in ProjectManager.getInstance().openProjects) {
      if (ProjectUtil.isSameProject(location, project)) {
        val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.directory.already.taken.error", project.name)
        return ValidationInfo(message)
      }
    }

    val file = File(location)
    if (file.exists()) {
      if (!file.canWrite()) {
        val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.directory.not.writable.error")
        return ValidationInfo(message)
      }
      val children = file.list()
      if (children == null) {
        val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.file.not.directory.error")
        return ValidationInfo(message)
      }
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        if (children.isNotEmpty()) {
          val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.directory.not.empty.warning")
          return ValidationInfo(message).asWarning().withOKEnabled()
        }
      }
    }
    return null
  }

  protected open fun validateParent(): ValidationInfo? = null

  override fun updateDataModel() {
    val location = location
    context.projectName = entityName
    context.projectFileDirectory = location
    createDirectory(File(location))
    updateProjectData()
  }

  abstract fun updateProjectData()

  companion object {
    private val EMPTY_VIEW = object : DataView<Nothing>() {
      override val data: Nothing by lazy { throw UnsupportedOperationException() }
      override val location: String = ""
      override val icon: Nothing by lazy { throw UnsupportedOperationException() }
      override val presentationName: String = "<None>"
      override val groupId: String = "org.example"
      override val version: String = "1.0-SNAPSHOT"

      override val isPresent: Boolean = false
    }

    private fun <T> ObservableClearableProperty<T>.asBinding() = PropertyBinding(::get, ::set)
  }
}
