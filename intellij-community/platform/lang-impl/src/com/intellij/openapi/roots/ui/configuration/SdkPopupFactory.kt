// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.AnActionButton.AnActionEventWrapper
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.list.ComboBoxPopup
import org.jetbrains.annotations.Contract
import java.awt.Component
import java.awt.Point
import java.awt.Window
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel


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
object SdkPopupFactory {
  @JvmStatic
  fun newBuilder() = SdkPopupBuilder.newBuilder()
}

/**
 * Represents Sdk selector popup
 * @see SdkPopupFactory
 */
interface SdkPopup {
  fun showInFocusCenter()
  fun showPopup(e: AnActionEvent)
  fun showUnderneathToTheRightOf(component: Component)
  fun showInBestPositionFor(editor: Editor)
}

@Suppress("DataClassPrivateConstructor")
data class SdkPopupBuilder private constructor(
  val project: Project? = null,
  val projectSdksModel: ProjectSdksModel? = null,
  val sdkListModelBuilder: SdkListModelBuilder? = null,

  val sdkTypeFilter: Condition<SdkTypeId>? = null,
  val sdkTypeCreateFilter: Condition<SdkTypeId>? = null,
  val sdkFilter: Condition<Sdk>? = null,

  val registerNewSdk : Boolean = false,
  val updateProjectSdk : Boolean = false,
  val updateSdkForFile: VirtualFile? = null,

  val onItemSelected: Consumer<SdkListItem>? = null,
  val onClosed: Runnable? = null
) : SdkPopup {

  companion object {
    internal fun newBuilder() = SdkPopupBuilder()
  }

  /**
   * Adds the newly created SDK into the [ProjectJdkTable] automatically.
   * In some cases it is not needed, this feature is disabled by default
   */
  @Contract(pure = true)
  fun registerNewSdk() = copy(registerNewSdk = true)

  /**
   * Registers the selected SDK as project SDK.
   */
  @Contract(pure = true)
  fun updateProjectSdkFromSelection() = copy(updateProjectSdk = true)

  /**
   * Sets a custom module SDK to the selected SDK if there is module and the module uses custom SDK,
   * sets the project SDK otherwise.
   */
  @Contract(pure = true)
  fun updateSdkForFile(file: PsiFile): SdkPopupBuilder {
    val virtualFile = file.virtualFile
    return copy(updateSdkForFile = virtualFile, updateProjectSdk = virtualFile == null)
  }

  /**
   * Sets a custom module SDK to the selected SDK if there is module and the module uses custom SDK,
   * sets the project SDK otherwise.
   */
  @Contract(pure = true)
  fun updateSdkForFile(file: VirtualFile) = copy(updateSdkForFile = file)

  @Contract(pure = true)
  fun withProject(project: Project?) = copy(project = project)

  @Contract(pure = true)
  fun withProjectSdksModel(projectSdksModel: ProjectSdksModel) = copy(projectSdksModel = projectSdksModel)

  @Contract(pure = true)
  fun withSdkListModelBuilder(sdkListModelBuilder: SdkListModelBuilder) = copy(sdkListModelBuilder = sdkListModelBuilder)

  @Contract(pure = true)
  fun withSdkType(type: SdkTypeId) = withSdkTypeFilter(Condition { sdk -> sdk == type })

  @Contract(pure = true)
  fun withSdkTypeFilter(filter: Condition<SdkTypeId>) = copy(sdkTypeFilter = filter)

  @Contract(pure = true)
  fun withSdkTypeCreateFilter(filter: Condition<SdkTypeId>) = copy(sdkTypeCreateFilter = filter)

  @Contract(pure = true)
  fun withSdkFilter(filter: Condition<Sdk>) = copy(sdkFilter = filter)

  /**
   * Executed when an item is selected in the popup (and popup is closing),
   * it is not executed if a popup was cancelled
   */
  @Contract(pure = true)
  fun onItemSelected(onItemSelected: Consumer<SdkListItem>) = copy(onItemSelected = onItemSelected)

  /**
   * Executed on popup is closed, independently from the result
   */
  @Contract(pure = true)
  fun onClosed(onClosed: Runnable) = copy(onClosed = onClosed)

  @Contract(pure = true)
  fun onSdkSelected(onItemSelected: Consumer<Sdk>) = onItemSelected(
    Consumer {
      if (it is SdkListItem.SdkItem) {
        onItemSelected.accept(it.sdk)
      }
    }
  )

  @Contract(pure = true)
  private fun build(): SdkPopup {
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
        if ((updateSdkForFile != null || registerNewSdk || updateProjectSdk) && item is SdkListItem.SdkItem) {
          val sdk = item.sdk
          runWriteAction {
            val jdkTable = ProjectJdkTable.getInstance()
            if (jdkTable.findJdk(sdk.name) == null) {
              jdkTable.addJdk(sdk)
            }

            fun setProjectSdk() {
              requireNotNull(project) { "project must be set to use " + ::updateProjectSdkFromSelection.name }
              ProjectRootManager.getInstance(project).projectSdk = sdk
            }

            if (updateProjectSdk) {
              setProjectSdk()
            }

            if (updateSdkForFile != null) {
              requireNotNull(project) { "project must be set to use " + ::updateProjectSdkFromSelection.name }

              val module = ModuleUtilCore.findModuleForFile(updateSdkForFile, project)
              if (module != null) {
                val moduleManager = ModuleRootManager.getInstance(module)
                if (moduleManager.isSdkInherited) {
                  if (!updateProjectSdk) {
                    setProjectSdk()
                  }
                } else {
                  moduleManager.modifiableModel.also {
                    it.sdk = sdk
                    it.commit()
                  }
                }
              }
            }
          }
        }

        onItemSelected?.accept(item)
      }

      override fun onExistingItemSelected(item: SdkListItem) {
        onItemSelected?.accept(item)
      }

      override fun onClosed() {
        onClosed?.run()
      }
    }

    return createSdkPopup(project, sdksModel, ownsModel, modelBuilder, popupListener)
  }

  override fun showInBestPositionFor(editor: Editor) = build().showInBestPositionFor(editor)
  override fun showInFocusCenter() = build().showInFocusCenter()
  override fun showPopup(e: AnActionEvent) = build().showPopup(e)
  override fun showUnderneathToTheRightOf(component: Component) = build().showUnderneathToTheRightOf(component)
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

private fun createSdkPopup(
  project: Project?,
  projectSdksModel: ProjectSdksModel,
  ownsProjectSdksModel: Boolean,
  myModelBuilder: SdkListModelBuilder,
  listener: SdkPopupListener
): SdkPopup {
  lateinit var popup: SdkPopupImpl
  val context = SdkListItemContext(project, projectSdksModel)

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
    if (e is AnActionEventWrapper) {
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
  private val myProject: Project?,
  mySdksModel: ProjectSdksModel
) : ComboBoxPopup.Context<SdkListItem> {
  var myModel = SdkListModel(true, emptyList())

  private val myRenderer = object : SdkListPresenter(mySdksModel) {
    override fun getModel(): SdkListModel {
      return myModel
    }
  }

  override fun getProject() = myProject
  override fun getMaximumRowCount() = 30
  override fun getModel() = myModel
  override fun getRenderer() = myRenderer
}

