// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.ActionHandler
import org.jetbrains.annotations.Contract
import java.awt.Component
import java.util.function.Consumer


/**
 * Allows to create and show Sdk selector popup.
 * The implementation shows:
 * - current SDKs
 * - locally detected SDKs
 * - download actions
 *
 * Filters can be used to remove unneeded elements
 * from the popup. The implementation automatically ignores
 * `SimpleJavaSdkType` if the `JavaSdkTypeImpl` is available
 *
 * ProjectJdkTable is not updated by default to attach a newly created SDK
 *
 * To show the popup, call one of the `SdkPopupBuilder.show*` methods
 */
interface SdkPopupFactory {
  fun createBuilder() : SdkPopupBuilder
  fun createPopup(builder: SdkPopupBuilder): SdkPopup
  fun createEditorNotificationPanelHandler(builder: SdkPopupBuilder): ActionHandler

  companion object {
    @JvmStatic
    fun newBuilder() = service<SdkPopupFactory>().createBuilder()
  }
}

/**
 * Represents Sdk selector popup
 * @see SdkPopupFactory
 */
interface SdkPopup : JBPopup {
  fun showPopup(e: AnActionEvent)
  fun showUnderneathToTheRightOf(component: Component)
}

interface SdkPopupBuilder {
  /**
   * Terminal operator of the builder to create a JBPopup instance for
   * that SDK popup
   */
  @Contract(pure = true)
  fun buildPopup(): SdkPopup

  /**
   * Terminal operator that returns an [EditorNotificationPanel] action handler.
   * The handler implementation builds a popup lazily for each interface call
   */
  @Contract(pure = true)
  fun buildEditorNotificationPanelHandler() : ActionHandler

  /**
   * Adds the newly created SDK into the [ProjectJdkTable] automatically.
   * In some cases it is not needed, this feature is disabled by default
   */
  @Contract(pure = true)
  fun registerNewSdk(): SdkPopupBuilder

  /**
   * Registers the selected SDK as project SDK.
   */
  @Contract(pure = true)
  fun updateProjectSdkFromSelection(): SdkPopupBuilder

  /**
   * Sets a custom module SDK to the selected SDK if there is module and the module uses custom SDK,
   * sets the project SDK otherwise.
   */
  @Contract(pure = true)
  fun updateSdkForFile(file: PsiFile): SdkPopupBuilder

  /**
   * Sets a custom module SDK to the selected SDK if there is module and the module uses custom SDK,
   * sets the project SDK otherwise.
   */
  @Contract(pure = true)
  fun updateSdkForFile(file: VirtualFile): SdkPopupBuilder

  @Contract(pure = true)
  fun withProject(project: Project?): SdkPopupBuilder

  @Contract(pure = true)
  fun withProjectSdksModel(projectSdksModel: ProjectSdksModel): SdkPopupBuilder

  @Contract(pure = true)
  fun withSdkListModelBuilder(sdkListModelBuilder: SdkListModelBuilder): SdkPopupBuilder

  @Contract(pure = true)
  fun withSdkType(type: SdkTypeId): SdkPopupBuilder

  @Contract(pure = true)
  fun withSdkTypeFilter(filter: Condition<SdkTypeId>): SdkPopupBuilder

  @Contract(pure = true)
  fun withSdkTypeCreateFilter(filter: Condition<SdkTypeId>): SdkPopupBuilder

  @Contract(pure = true)
  fun withSdkFilter(filter: Condition<Sdk>): SdkPopupBuilder

  /**
   * Executed when an item is selected in the popup (and popup is closing),
   * it is not executed if a popup was cancelled
   */
  @Contract(pure = true)
  fun onItemSelected(onItemSelected: Consumer<SdkListItem>): SdkPopupBuilder

  /**
   * Executed on popup is closed, independently from the result
   */
  @Contract(pure = true)
  fun onPopupClosed(onClosed: Runnable): SdkPopupBuilder

  @Contract(pure = true)
  fun onSdkSelected(onSdkSelected: Consumer<Sdk>): SdkPopupBuilder
}
