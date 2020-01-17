// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.DependentSdkType
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBoxPopupState
import com.intellij.openapi.util.Condition
import java.util.function.Predicate
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
    @JvmOverloads
    fun createSdkComboBoxModel(
      project: Project,
      sdksModel: ProjectSdksModel,
      sdkTypeFilter: Predicate<SdkTypeId>? = null,
      sdkTypeCreationFilter: Predicate<SdkTypeId>? = null,
      sdkFilter: Predicate<Sdk>? = null
    ): SdkComboBoxModel {
      val selectedItem = SdkListItem.NoneSdkItem()
      val sdkTypeCondition = sdkTypeFilter?.let { f -> Condition<SdkTypeId> { f.test(it) } }
      val sdkTypeCreationCondition = sdkTypeCreationFilter?.let { f -> Condition<SdkTypeId> { f.test(it) } }
      val sdkCondition = sdkFilter?.let { f -> Condition<Sdk> { f.test(it) } }
      val modelBuilder = SdkListModelBuilder(project, sdksModel, sdkTypeCondition, sdkTypeCreationCondition, sdkCondition)
      if (sdksModel.projectSdk != null) modelBuilder.showProjectSdkItem()
      val listModel = modelBuilder.buildModel()
      return SdkComboBoxModel(selectedItem, project, sdksModel, listModel, modelBuilder)
    }

    @JvmStatic
    @JvmOverloads
    @Suppress("NAME_SHADOWING")
    fun createJdkComboBoxModel(
      project: Project,
      sdksModel: ProjectSdksModel,
      sdkTypeFilter: Predicate<SdkTypeId>? = null,
      sdkTypeCreationFilter: Predicate<SdkTypeId>? = null,
      sdkFilter: Predicate<Sdk>? = null
    ): SdkComboBoxModel {
      val sdkTypeFilter = Predicate<SdkTypeId> {
        it is JavaSdkType && it !is DependentSdkType &&
        (sdkTypeFilter == null || sdkTypeFilter.test(it))
      }
      val noJavaSdkTypes = {
        SdkType.getAllTypes()
          .filter { sdkTypeFilter.test(it) }
          .filterNot { it is SimpleJavaSdkType }
          .isEmpty()
      }
      val sdkTypeCreationFilter = Predicate<SdkTypeId> {
        (it !is SimpleJavaSdkType || noJavaSdkTypes()) &&
        (sdkTypeCreationFilter == null || sdkTypeCreationFilter.test(it))
      }
      return createSdkComboBoxModel(project, sdksModel, sdkTypeFilter, sdkTypeCreationFilter, sdkFilter)
    }
  }
}