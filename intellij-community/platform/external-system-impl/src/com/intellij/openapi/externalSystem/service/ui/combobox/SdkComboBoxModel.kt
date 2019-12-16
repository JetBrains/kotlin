// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui.combobox

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.isValidJdk
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkListModel
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.openapi.ui.ComboBoxPopupState
import com.intellij.openapi.util.Condition
import javax.swing.ComboBoxModel
import javax.swing.ListModel

class SdkComboBoxModel private constructor(
  private var selectedItem: SdkListItem,
  val project: Project,
  val sdksModel: ProjectSdksModel,
  val listModel: SdkListModel,
  val modelBuilder: SdkListModelBuilder
) : ComboBoxModel<SdkListItem>, ListModel<SdkListItem> by listModel, ComboBoxPopupState<SdkListItem> by listModel {

  override fun getSelectedItem() = selectedItem

  override fun setSelectedItem(anItem: Any?) {
    selectedItem = when (anItem) {
      null -> SdkListItem.NoneSdkItem()
      is SdkListItem -> anItem
      else -> throw UnsupportedOperationException("Unsupported item $anItem")
    }
  }

  fun copyAndSetListModel(listModel: SdkListModel): SdkComboBoxModel {
    return SdkComboBoxModel(selectedItem, project, sdksModel, listModel, modelBuilder)
  }

  companion object {
    @JvmStatic
    fun createSdkComboBoxModel(
      project: Project,
      sdksModel: ProjectSdksModel,
      sdkTypeFilter: Condition<SdkTypeId>? = null,
      sdkTypeCreationFilter: Condition<SdkTypeId>? = null,
      sdkFilter: Condition<Sdk>? = null
    ): SdkComboBoxModel {
      val selectedItem = SdkListItem.NoneSdkItem()
      val modelBuilder = SdkListModelBuilder(project, sdksModel, sdkTypeFilter, sdkTypeCreationFilter, sdkFilter)
      if (sdksModel.projectSdk != null) modelBuilder.showProjectSdkItem()
      val listModel = modelBuilder.buildModel()
      return SdkComboBoxModel(selectedItem, project, sdksModel, listModel, modelBuilder)
    }

    @JvmStatic
    fun createJdkComboBoxModel(project: Project, sdksModel: ProjectSdksModel): SdkComboBoxModel {
      val sdkTypeFilter = Condition<SdkTypeId> { it is JavaSdkType }
      val noJavaSdkTypes = { SdkType.getAllTypes().filterNot { it is SimpleJavaSdkType }.isEmpty() }
      val sdkTypeCreationFilter = Condition<SdkTypeId> { noJavaSdkTypes() || it !is SimpleJavaSdkType }
      val sdkFilter = Condition<Sdk> { SdkDownloadTracker.getInstance().isDownloading(it) || isValidJdk(it.homePath) }
      return createSdkComboBoxModel(project, sdksModel, sdkTypeFilter, sdkTypeCreationFilter, sdkFilter)
    }
  }
}