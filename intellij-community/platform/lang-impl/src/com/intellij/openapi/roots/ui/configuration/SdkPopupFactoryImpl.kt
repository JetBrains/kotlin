// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.AnActionButton
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.ActionHandler
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.list.ComboBoxPopup
import java.awt.Component
import java.awt.Point
import java.awt.Window
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

private data class SdkPopupBuilderImpl(
  val project: Project? = null,
  val projectSdksModel: ProjectSdksModel? = null,
  val sdkListModelBuilder: SdkListModelBuilder? = null,

  val sdkTypeFilter: Condition<SdkTypeId>? = null,
  val sdkTypeCreateFilter: Condition<SdkTypeId>? = null,
  val sdkFilter: Condition<Sdk>? = null,

  val registerNewSdk : Boolean = false,
  val updateProjectSdk : Boolean = false,

  val updateSdkForVirtualFile: VirtualFile? = null,
  val updateSdkForPsiFile: PsiFile? = null,

  val onItemSelected: Consumer<SdkListItem>? = null,
  val onSdkSelected: Consumer<Sdk>? = null,
  val onPopupClosed: Runnable? = null
) : SdkPopupBuilder {
  override fun registerNewSdk() = copy(registerNewSdk = true)
  override fun updateProjectSdkFromSelection() = copy(updateProjectSdk = true)
  override fun updateSdkForFile(file: PsiFile) = copy(updateSdkForPsiFile = file)
  override fun updateSdkForFile(file: VirtualFile) = copy(updateSdkForVirtualFile = file)
  override fun withProject(project: Project?) = copy(project = project)
  override fun withProjectSdksModel(projectSdksModel: ProjectSdksModel) = copy(projectSdksModel = projectSdksModel)
  override fun withSdkListModelBuilder(sdkListModelBuilder: SdkListModelBuilder) = copy(sdkListModelBuilder = sdkListModelBuilder)
  override fun withSdkType(type: SdkTypeId) = withSdkTypeFilter(Condition { sdk -> sdk == type })
  override fun withSdkTypeFilter(filter: Condition<SdkTypeId>) = copy(sdkTypeFilter = filter)
  override fun withSdkTypeCreateFilter(filter: Condition<SdkTypeId>) = copy(sdkTypeCreateFilter = filter)
  override fun withSdkFilter(filter: Condition<Sdk>) = copy(sdkFilter = filter)
  override fun onItemSelected(onItemSelected: Consumer<SdkListItem>) = copy(onItemSelected = onItemSelected)
  override fun onPopupClosed(onClosed: Runnable) = copy(onPopupClosed = onClosed)
  override fun onSdkSelected(onSdkSelected: Consumer<Sdk>) = copy(onSdkSelected = onSdkSelected)
  override fun buildPopup() = service<SdkPopupFactory>().createPopup(copy())
  override fun buildEditorNotificationPanelHandler(): ActionHandler = service<SdkPopupFactory>().createEditorNotificationPanelHandler(copy())
}

private interface SdkPopupListener {
  fun onClosed()

  /**
   * Executed when a new item was created via a user action
   * and added to the model, called after model is refreshed
   */
  fun onNewItemAddedAndSelected(item: SdkListItem)

  /**
   * Executed when an existing selectable item was selected
   * in the popup, it does mean no new items were created
   * by a user
   */
  fun onExistingItemSelected(item: SdkListItem)
}

internal class PlatformSdkPopupFactory : SdkPopupFactory {
  override fun createBuilder(): SdkPopupBuilder = SdkPopupBuilderImpl()

  override fun createEditorNotificationPanelHandler(builder: SdkPopupBuilder) = object : ActionHandler {
    override fun handlePanelActionClick(panel: EditorNotificationPanel,
                                        event: HyperlinkEvent) {
      //FileEditorManager#addTopComponent wraps the panel to implement borders, unwrapping
      val anchorPanel = when (val parent = panel.parent) {
        is JComponent -> parent
        else -> panel
      }

      createPopup(builder).showUnderneathToTheRightOf(anchorPanel)
    }

    override fun handleQuickFixClick(editor: Editor, file: PsiFile) {
      createPopup(builder).showInBestPositionFor(editor)
    }
  }

  override fun createPopup(builder: SdkPopupBuilder): SdkPopup = (builder as SdkPopupBuilderImpl).copy().run {
    val (sdksModel, ownsModel) = if (projectSdksModel != null) {
      projectSdksModel to false
    }
    else {
      ProjectSdksModel() to true
    }

    val modelBuilder = if (sdkListModelBuilder != null) {
      require(sdkTypeFilter == null) { "sdkListModelBuilder was set explicitly via " + ::withSdkListModelBuilder.name }
      require(sdkTypeCreateFilter == null) { "sdkListModelBuilder was set explicitly via " + ::withSdkListModelBuilder.name }
      require(sdkFilter == null) { "sdkListModelBuilder was set explicitly via " + ::withSdkListModelBuilder.name }
      sdkListModelBuilder
    }
    else {
      SdkListModelBuilder(
        project,
        sdksModel,
        sdkTypeFilter,
        sdkTypeCreateFilter,
        sdkFilter
      )
    }

    if (updateProjectSdk && project == null) {
      require(false) { "Cannot update project SDK when project was not set" }
    }

    val popupListener = object : SdkPopupListener {
      override fun onNewItemAddedAndSelected(item: SdkListItem) {
        val addToJdkTable = registerNewSdk || updateProjectSdk || updateSdkForPsiFile != null || updateSdkForVirtualFile != null

        if (addToJdkTable && item is SdkListItem.SdkItem) {
          val sdk = item.sdk
          runWriteAction {
            val jdkTable = ProjectJdkTable.getInstance()
            if (jdkTable.findJdk(sdk.name) == null) {
              jdkTable.addJdk(sdk)
            }
          }
        }

        onItemSelected(item)
      }

      override fun onExistingItemSelected(item: SdkListItem) {
        onItemSelected(item)
      }

      private fun onItemSelected(item: SdkListItem) {
        onItemSelected?.accept(item)

        if (item is SdkListItem.SdkItem) {
          onSdkSelected(item.sdk)
        }
      }

      private fun onSdkSelected(sdk: Sdk) {
        onSdkSelected?.accept(sdk)

        if (updateProjectSdk) {
          runWriteAction {
            requireNotNull(project) { "project must be set to use " + SdkPopupBuilder::updateProjectSdkFromSelection.name }
            ProjectRootManager.getInstance(project).projectSdk = sdk
          }
        }

        if (updateSdkForPsiFile != null || updateSdkForVirtualFile != null) {
          requireNotNull(project) { "project must be set to use " + ::updateProjectSdkFromSelection.name }

          runWriteAction {
            val moduleToUpdateSdk = when {
              updateSdkForVirtualFile != null -> ModuleUtilCore.findModuleForFile(updateSdkForVirtualFile, project)
              updateSdkForPsiFile != null -> ModuleUtilCore.findModuleForFile(updateSdkForPsiFile)
              else -> null
            }

            if (moduleToUpdateSdk != null) {
              val roots = ModuleRootManager.getInstance(moduleToUpdateSdk)
              if (!roots.isSdkInherited) {
                roots.modifiableModel.also {
                  it.sdk = sdk
                  it.commit()
                }
                return@runWriteAction
              }
            }

            ProjectRootManager.getInstance(project).projectSdk = sdk
          }
        }
      }

      override fun onClosed() {
        onPopupClosed?.run()
      }
    }

    return createSdkPopup(project, sdksModel, ownsModel, modelBuilder, popupListener)
  }

  private fun createSdkPopup(
    project: Project?,
    projectSdksModel: ProjectSdksModel,
    ownsProjectSdksModel: Boolean,
    myModelBuilder: SdkListModelBuilder,
    listener: SdkPopupListener
  ): SdkPopup {
    lateinit var popup: SdkPopupImpl
    val context = SdkListItemContext(project)

    val onItemSelected = Consumer<SdkListItem> { value ->
      myModelBuilder.processSelectedElement(popup.popupOwner, value,
                                            listener::onNewItemAddedAndSelected,
                                            listener::onExistingItemSelected
      )
    }

    popup = SdkPopupImpl(context, onItemSelected)

    val modelListener: SdkListModelBuilder.ModelListener = object : SdkListModelBuilder.ModelListener {
      override fun syncModel(model: SdkListModel) {
        context.myModel = model
        popup.syncWithModelChange()
      }
    }

    myModelBuilder.addModelListener(modelListener)

    popup.addListener(object : JBPopupListener {
      override fun beforeShown(event: LightweightWindowEvent) {
        if (ownsProjectSdksModel) {
          projectSdksModel.reset(project)
        }
        myModelBuilder.reloadActions()
        myModelBuilder.detectItems(popup.list, popup)
        myModelBuilder.reloadSdks()
      }

      override fun onClosed(event: LightweightWindowEvent) {
        myModelBuilder.removeListener(modelListener)
        listener.onClosed()
      }
    })

    return popup
  }
}

private class SdkPopupImpl(
  context: SdkListItemContext,
  onItemSelected: Consumer<SdkListItem>
) : ComboBoxPopup<SdkListItem>(context, null, onItemSelected), SdkPopup {
  val popupOwner: JComponent
    get() {
      val owner = myOwner
      if (owner is JComponent) {
        return owner
      }

      if (owner is JFrame) {
        owner.rootPane?.let { return it }
      }

      if (owner is Window) {
        owner.components.first { it is JComponent }?.let { return it as JComponent }
      }

      return JPanel()
    }

  override fun showPopup(e: AnActionEvent) {
    if (e is AnActionButton.AnActionEventWrapper) {
      e.showPopup(this)
    }
    else {
      showInBestPositionFor(e.dataContext)
    }
  }

  override fun showUnderneathToTheRightOf(component: Component) {
    val popupWidth = list.preferredSize.width
    show(RelativePoint(component, Point(component.width - popupWidth, component.height)))
  }
}

private class SdkListItemContext(
  private val myProject: Project?
) : ComboBoxPopup.Context<SdkListItem> {
  var myModel = SdkListModel.emptyModel()

  private val myRenderer = SdkListPresenter { myModel }

  override fun getProject() = myProject
  override fun getMaximumRowCount() = 30
  override fun getModel() = myModel
  override fun getRenderer() = myRenderer
}
