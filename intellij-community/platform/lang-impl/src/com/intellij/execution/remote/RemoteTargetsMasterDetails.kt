// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.remote.java.JavaLanguageRuntimeType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.util.IconUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.containers.toArray
import com.intellij.util.text.UniqueNameGenerator

class RemoteTargetsMasterDetails @JvmOverloads constructor(private val project: Project, private val initialSelectedName: String? = null)
  : MasterDetailsComponent() {

  init {
    // note that `MasterDetailsComponent` does not work without `initTree()`
    initTree()
  }

  override fun getDisplayName(): String = "Remote Targets"

  override fun getEmptySelectionString(): String? {
    return "To add new target, click +"
  }

  override fun reset() {
    myRoot.removeAllChildren()

    allTargets().forEach { nextTarget -> addTargetNode(nextTarget) }

    super.reset()

    initialSelectedName?.let { selectNodeInTree(initialSelectedName) }
  }

  override fun isModified(): Boolean =
    allTargets().size != getConfiguredTargets().size ||
    !deletedTargets().isEmpty() ||
    super.isModified()

  override fun createActions(fromPopup: Boolean): List<AnAction> = mutableListOf(
    CreateNewTargetGroup(),
    MyDeleteAction(),
    DuplicateAction(),
    NewTargetWithJavaWizardGroup()
  )

  override fun processRemovedItems() {
    val deletedTargets = deletedTargets()
    deletedTargets.forEach { RemoteTargetsManager.instance.targets.removeConfig(it) }
    super.processRemovedItems()
  }

  override fun wasObjectStored(editableObject: Any?): Boolean {
    return RemoteTargetsManager.instance.targets.resolvedConfigs().contains(editableObject)
  }

  private fun deletedTargets(): Set<RemoteTargetConfiguration> = allTargets().toSet() - getConfiguredTargets()

  override fun apply() {
    super.apply()

    val addedConfigs = getConfiguredTargets() - RemoteTargetsManager.instance.targets.resolvedConfigs()
    addedConfigs.forEach { RemoteTargetsManager.instance.targets.addConfig(it) }
  }

  private fun allTargets() = RemoteTargetsManager.instance.targets.resolvedConfigs()

  private fun addTargetNode(config: RemoteTargetConfiguration): MyNode {
    val configurable = RemoteTargetDetailsConfigurable(project, config)
    val node = MyNode(configurable)
    addNode(node, myRoot)
    selectNodeInTree(node)
    return myRoot
  }

  private fun getConfiguredTargets(): List<RemoteTargetConfiguration> =
    myRoot.children().asSequence()
      .map { node -> (node as MyNode).configurable?.editableObject as? RemoteTargetConfiguration }
      .filterNotNull()
      .toList()

  private fun applyUniqueName(config: RemoteTargetConfiguration) {
    config.displayName = UniqueNameGenerator.generateUniqueName(config.getTargetType().displayName) { curName ->
      getConfiguredTargets().none { it.displayName == curName }
    }
  }

  private inner class CreateNewTargetAction(private val type: RemoteTargetType<*>)
    : DumbAwareAction(type.displayName, null, type.icon) {

    override fun actionPerformed(e: AnActionEvent) {
      val newConfig = type.createDefaultConfig()
      applyUniqueName(newConfig)
      val newNode = addTargetNode(newConfig)
      selectNodeInTree(newNode, true, true)
    }
  }

  private inner class OpenNewTargetWizardAction(private val target: RemoteTargetType<*>, private val runtime: LanguageRuntimeType<*>?)
    : DumbAwareAction(target.displayName + "...", null, target.icon) {

    override fun actionPerformed(e: AnActionEvent) {
      val wizard = RemoteTargetWizard.createWizard(e.project!!, target, runtime)
      if (wizard?.showAndGet() == true) {
        val newConfig = target.createDefaultConfig()
        applyUniqueName(newConfig)
        val newNode = addTargetNode(newConfig)
        selectNodeInTree(newNode, true, true)
      }
    }
  }

  private inner class CreateNewTargetGroup : ActionGroup("Add", "", IconUtil.getAddIcon()),
                                             ActionGroupWithPreselection, DumbAware {
    init {
      registerCustomShortcutSet(CommonShortcuts.INSERT, myTree)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
      return RemoteTargetType.EXTENSION_NAME.extensionList
        .map { CreateNewTargetAction(it) }
        .toArray(AnAction.EMPTY_ARRAY)
    }

    override fun getActionGroup(): ActionGroup {
      return this
    }
  }

  private inner class NewTargetWithJavaWizardGroup : ActionGroup("New Target with Java...", "", PlatformIcons.JAR_ICON),
                                                     ActionGroupWithPreselection, DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
      val project = e?.project
      if (project == null) return AnAction.EMPTY_ARRAY

      val javaLanguageRuntime = LanguageRuntimeType.EXTENSION_NAME.extensionList
        .firstOrNull { it is JavaLanguageRuntimeType }

      return RemoteTargetType.EXTENSION_NAME.extensionList
        .filter { it.providesNewWizard(project, javaLanguageRuntime) }
        .map { OpenNewTargetWizardAction(it, javaLanguageRuntime) }
        .toArray(AnAction.EMPTY_ARRAY)
    }

    override fun getActionGroup(): ActionGroup {
      return this
    }
  }

  private inner class DuplicateAction : DumbAwareAction("Duplicate", "Duplicate", PlatformIcons.COPY_ICON) {
    override fun update(e: AnActionEvent) {
      templatePresentation.isEnabled = getSelectedTarget() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
      duplicateSelected()?.let { copy ->
        applyUniqueName(copy)
        RemoteTargetsManager.instance.targets.addConfig(copy)
        val newNode = addTargetNode(copy)
        selectNodeInTree(newNode, true, true)
      }
    }

    private fun duplicateSelected(): RemoteTargetConfiguration? =
      getSelectedTarget()?.let { it.getTargetType().duplicateConfig(it) }

    private fun getSelectedTarget() = selectedNode.configurable?.editableObject as? RemoteTargetConfiguration
  }
}