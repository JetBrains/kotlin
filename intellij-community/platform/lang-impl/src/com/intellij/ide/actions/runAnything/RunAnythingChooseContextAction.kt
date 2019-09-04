// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore.getModuleDirPath
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ErrorLabel
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Paths
import java.util.function.Supplier
import javax.swing.*

abstract class RunAnythingChooseContextAction : CustomComponentAction, DumbAware, ActionGroup() {
  override fun canBePerformed(context: DataContext): Boolean = true
  override fun isPopup(): Boolean = true
  override fun getChildren(e: AnActionEvent?): Array<AnAction> = EMPTY_ARRAY

  open var currentContext: RunAnythingContext = RunAnythingContext.ProjectContext

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = currentContext.presentation.label
    e.presentation.icon = currentContext.presentation.icon
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }

    val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
    if (component == null) {
      return
    }

    val dataContext = e.dataContext
    val actionItems = PopupFactoryImpl.ActionGroupPopup.getActionItems(createGroup(project, component), dataContext, false, false, true,
                                                                       true, ActionPlaces.POPUP)

    ChooseContextPopup(ChooseContextPopupStep(actionItems, dataContext), dataContext)
      .also { it.size = Dimension(500, 300) }
      .showUnderneathOf(component)
  }

  private fun createGroup(project: Project, component: JComponent): ActionGroup {
    val updateToolbar = {
      val toolbar = UIUtil.uiParents(component, true).filter(ActionToolbar::class.java).first()
      toolbar!!.updateActionsImmediately()
    }

    return DefaultActionGroup(createItems(project, updateToolbar))
  }

  private fun createItems(project: Project, updateToolbar: () -> Unit): List<AnAction> {
    return sequenceOf<AnAction>(ProjectItem(project, updateToolbar))
      .plus(BrowseDirectoryItem(updateToolbar))
      .plus(RunAnythingContextRecentDirectoryCache.getInstance(project).state.paths.reversed().map<String, AnAction> {
        RecentDirectoryItem(it, updateToolbar)
      })
      .plus(ModuleManager.getInstance(project).modules.map { ModuleItem(it, updateToolbar) }.let { if (it.size > 1) it else emptyList() })
      .toList()
  }

  inner class ModuleItem(val module: Module, updateToolbar: () -> Unit) : ContextItem(updateToolbar) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.ModuleContext(module))
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = module.name
      e.presentation.icon = AllIcons.Nodes.Module

      val moduleDirectory =
        ModuleRootManager.getInstance(module).contentRoots.let { if (it.size == 1) it[0].path else getModuleDirPath(module) }

      e.presentation.description =
        module.project.guessProjectDir()?.let { FileUtil.getRelativePath(it.path, moduleDirectory, '/') } ?: "undefined"
    }
  }

  inner class ProjectItem(val project: Project, updateToolbar: () -> Unit) : ContextItem(updateToolbar) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.ProjectContext)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = IdeBundle.message("run.anything.context.project")
      e.presentation.description = project.basePath
      e.presentation.icon = EmptyIcon.ICON_16
    }
  }

  inner class BrowseDirectoryItem(updateToolbar: () -> Unit) : ContextItem(updateToolbar) {
    override fun actionPerformed(e: AnActionEvent) {
      ApplicationManager.getApplication().invokeLater {
        val project = e.project!!
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().also { it.isForcedToUseIdeaFileChooser = true }
        FileChooserFactory.getInstance().createPathChooser(descriptor, project, e.dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT))
          .choose(project.guessProjectDir()) {
            val recentDirectories = RunAnythingContextRecentDirectoryCache.getInstance(project).state.paths
            val path = it.single().path
            if (recentDirectories.size >= Registry.intValue("run.anything.context.recent.directory.number")) {
              recentDirectories.removeAt(0)
            }
            recentDirectories.add(path)

            applyContext(RunAnythingContext.RecentDirectoryContext(path))
          }
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = IdeBundle.message("run.anything.context.browse.directory")
      e.presentation.description = ""
      e.presentation.icon = AllIcons.Actions.Menu_open
    }
  }

  inner class RecentDirectoryItem(val path: String, updateToolbar: () -> Unit) : ContextItem(updateToolbar) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.RecentDirectoryContext(path))
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = FileUtil.getLocationRelativeToUserHome(path)
      e.presentation.description = ""
      e.presentation.icon = AllIcons.Nodes.Folder
    }
  }

  abstract inner class ContextItem(val updateToolbar: () -> Unit) : AnAction() {
    fun applyContext(context: RunAnythingContext) {
      currentContext = context

      kotlin.run { updateToolbar }
    }
  }

  open inner class ChooseContextPopup(step: ActionPopupStep, dataContext: DataContext)
    : PopupFactoryImpl.ActionGroupPopup(null, step, null, dataContext, ActionPlaces.POPUP, -1) {
    override fun getListElementRenderer(): PopupListElementRenderer<PopupFactoryImpl.ActionItem> =
      object : PopupListElementRenderer<PopupFactoryImpl.ActionItem>(this) {
        private lateinit var myInfoLabel: JLabel

        override fun createItemComponent(): JComponent {
          myTextLabel = ErrorLabel()
          myInfoLabel = JLabel()
          myTextLabel.border = JBUI.Borders.emptyRight(10)

          val textPanel = JPanel(BorderLayout())
          textPanel.add(myTextLabel, BorderLayout.WEST)
          textPanel.add(myInfoLabel, BorderLayout.CENTER)
          return layoutComponent(textPanel)
        }

        override fun customizeComponent(list: JList<out PopupFactoryImpl.ActionItem>,
                                        actionItem: PopupFactoryImpl.ActionItem,
                                        isSelected: Boolean) {
          val event = ActionUtil.createEmptyEvent()
          ActionUtil.performDumbAwareUpdate(true, actionItem.action, event, false)

          val description = event.presentation.description
          if (description != null) {
            myInfoLabel.text = description
          }

          myTextLabel.text = event.presentation.text
          myInfoLabel.foreground = if (isSelected) UIUtil.getListSelectionForeground(true) else UIUtil.getInactiveTextColor()

        }
      }
  }

  class ChooseContextPopupStep(val actions: List<PopupFactoryImpl.ActionItem>, dataContext: DataContext)
    : ActionPopupStep(actions, IdeBundle.message("run.anything.context.title.working.directory"), Supplier { dataContext }, null, true,
                      Condition { false }, false, true) {

    override fun getSeparatorAbove(value: PopupFactoryImpl.ActionItem?): ListSeparator? {
      val action = value?.action
      when {
        action is BrowseDirectoryItem -> return ListSeparator(IdeBundle.message("run.anything.context.separator.directories"))
        action is ModuleItem && action == actions.filter { it.action is ModuleItem }[0].action ->
          return ListSeparator(IdeBundle.message("run.anything.context.separator.modules"))
        else -> return super.getSeparatorAbove(value)
      }
    }
  }

  data class ContextPresentation(val label: String, val description: String? = null, val icon: Icon? = null)

  abstract class RunAnythingContext(var presentation: ContextPresentation) {
    object ProjectContext : RunAnythingContext(ContextPresentation("Project"))

    data class ModuleContext(val module: Module) : RunAnythingContext(
      ContextPresentation(module.name, module.moduleFile?.parent.toString(), AllIcons.Actions.ModuleDirectory))

    data class RecentDirectoryContext(val path: String) : RunAnythingContext(
      ContextPresentation(FileUtil.getLocationRelativeToUserHome(Paths.get(path).toString()), icon = AllIcons.Nodes.Folder)
    )
  }
}