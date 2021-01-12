// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk

class SdkComboBoxTest : SdkComboBoxTestCase() {

  fun `test simple usage`() {
    val sdk1 = createAndRegisterSdk()
    val sdk2 = createAndRegisterSdk(isProjectSdk = true)
    val sdk3 = createAndRegisterSdk()
    val sdk4 = TestSdkGenerator.createNextSdk()
    val sdk5 = createAndRegisterDependentSdk()
    val comboBox = createJdkComboBox()

    assertComboBoxContent(comboBox)
      .item<SdkListItem.ProjectSdkItem> { assertEquals(sdk2, comboBox.getProjectSdk()) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk3, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk5.parent, it) }
      .nothing()

    assertComboBoxSelection<SdkListItem.NoneSdkItem>(comboBox, null)
    comboBox.setSelectedSdk(sdk1)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk1)
    comboBox.setSelectedSdk(sdk2)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk2)
    comboBox.setSelectedSdk(sdk3)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk3)
    comboBox.setSelectedSdk(sdk4)
    assertComboBoxSelection<SdkListItem.InvalidSdkItem>(comboBox, null)

    comboBox.setSelectedSdk(sdk1.name)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk1)
    comboBox.setSelectedSdk(sdk2.name)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk2)
    comboBox.setSelectedSdk(sdk3.name)
    assertComboBoxSelection<SdkListItem.SdkItem>(comboBox, sdk3)
    comboBox.setSelectedSdk(sdk4.name)
    assertComboBoxSelection<SdkListItem.InvalidSdkItem>(comboBox, null)

    comboBox.selectedItem = SdkListItem.NoneSdkItem()
    assertComboBoxSelection<SdkListItem.NoneSdkItem>(comboBox, null)

    assertComboBoxContent(comboBox)
      .item<SdkListItem.NoneSdkItem>(isSelected = true)
      .item<SdkListItem.ProjectSdkItem> { assertEquals(sdk2, comboBox.getProjectSdk()) }
      .item<SdkListItem.InvalidSdkItem> { assertEquals(sdk4.name, it.sdkName) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk3, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk5.parent, it) }
      .nothing()

    comboBox.setSelectedSdk(sdk5)
    assertComboBoxSelection<SdkListItem.InvalidSdkItem>(comboBox, null)
    assertComboBoxContent(comboBox)
      .item<SdkListItem.NoneSdkItem>()
      .item<SdkListItem.ProjectSdkItem> { assertEquals(sdk2, comboBox.getProjectSdk()) }
      .item<SdkListItem.InvalidSdkItem>(isSelected = true) { assertEquals(sdk5.name, it.sdkName) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk3, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk5.parent, it) }
      .nothing()

    comboBox.setSelectedItem(SdkListItem.ProjectSdkItem())
    assertComboBoxSelection<SdkListItem.ProjectSdkItem>(comboBox, sdk2)
    comboBox.setSelectedItem(SdkListItem.NoneSdkItem())
    assertComboBoxSelection<SdkListItem.NoneSdkItem>(comboBox, null)
    comboBox.setSelectedItem(SdkListItem.InvalidSdkItem("invalid sdk"))
    assertComboBoxSelection<SdkListItem.InvalidSdkItem>(comboBox, null)

    assertComboBoxContent(comboBox)
      .item<SdkListItem.NoneSdkItem>()
      .item<SdkListItem.ProjectSdkItem> { assertEquals(sdk2, comboBox.getProjectSdk()) }
      .item<SdkListItem.InvalidSdkItem>(isSelected = true) { assertEquals("invalid sdk", it.sdkName) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk3, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(sdk5.parent, it) }
      .nothing()
  }

  fun `test combobox actions`() {
    val comboBox = createJdkComboBox()
      .withOpenDropdownPopup()

    assertComboBoxContent(comboBox)
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.DOWNLOAD, TestSdkType, it) }
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.ADD, TestSdkType, it) }
      .nothing()

    val download1 = comboBox.touchDownloadAction()
    val download2 = comboBox.touchDownloadAction()
    val download3 = comboBox.touchDownloadAction()
    val download4 = comboBox.touchDownloadAction()

    assertComboBoxContent(comboBox)
      .item<SdkListItem.SdkItem> { assertSdkItem(download1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(download2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(download3, it) }
      .item<SdkListItem.SdkItem>(isSelected = true) { assertSdkItem(download4, it) }
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.DOWNLOAD, TestSdkType, it) }
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.ADD, TestSdkType, it) }
      .nothing()

    val add1 = comboBox.touchAddAction()
    val add2 = comboBox.touchAddAction()
    val add3 = comboBox.touchAddAction()
    val add4 = comboBox.touchAddAction()

    assertComboBoxContent(comboBox)
      .item<SdkListItem.SdkItem> { assertSdkItem(download1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(download2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(download3, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(download4, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(add1, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(add2, it) }
      .item<SdkListItem.SdkItem> { assertSdkItem(add3, it) }
      .item<SdkListItem.SdkItem>(isSelected = true) { assertSdkItem(add4, it) }
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.DOWNLOAD, TestSdkType, it) }
      .item<SdkListItem.ActionItem> { assertActionItem(SdkListItem.ActionRole.ADD, TestSdkType, it) }
      .nothing()

    assertCollectionContent(comboBox.model.sdksModel.projectSdks.values.sortedBy { it.name })
      .element<Sdk> { assertSdk(download1, it) }
      .element<Sdk> { assertSdk(download2, it) }
      .element<Sdk> { assertSdk(download3, it) }
      .element<Sdk> { assertSdk(download4, it) }
      .element<Sdk> { assertSdk(add1, it) }
      .element<Sdk> { assertSdk(add2, it) }
      .element<Sdk> { assertSdk(add3, it) }
      .element<Sdk> { assertSdk(add4, it) }
      .nothing()

    assertCollectionContent(ProjectJdkTable.getInstance().getSdksOfType(TestSdkType))
      .nothing()
  }
}