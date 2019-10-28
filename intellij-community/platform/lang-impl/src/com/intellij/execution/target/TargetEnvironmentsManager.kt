// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target

import com.intellij.openapi.components.*
import com.intellij.util.text.UniqueNameGenerator
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

@State(name = "RemoteTargetsManager", storages = [Storage("remote-targets.xml")])
class TargetEnvironmentsManager : PersistentStateComponent<TargetEnvironmentsManager.TargetsListState> {

  val targets: BaseExtendableList<TargetEnvironmentConfiguration, RemoteTargetType<*>> = TargetsList()

  override fun getState(): TargetsListState {
    val result = TargetsListState()
    for (next in this.targets.state.configs) {
      result.targets.add(next as OneTargetState)
    }
    return result
  }

  override fun loadState(state: TargetsListState) {
    targets.loadState(state.targets)
  }

  fun addTarget(target: TargetEnvironmentConfiguration) {
    if (!targets.resolvedConfigs().contains(target)) {
      ensureUniqueName(target)
      targets.addConfig(target)
    }
  }

  fun removeTarget(target: TargetEnvironmentConfiguration) {
    targets.removeConfig(target)
  }

  internal fun ensureUniqueName(target: TargetEnvironmentConfiguration) {
    if (!targets.resolvedConfigs().contains(target)) {
      val existingNames = targets.resolvedConfigs().map { it.displayName }.toSet() + targets.unresolvedNames()
      val uniqueName = UniqueNameGenerator.generateUniqueName(target.displayName, existingNames)
      target.displayName = uniqueName
    }
  }

  companion object {
    @JvmStatic
    val instance: TargetEnvironmentsManager = ServiceManager.getService(
      TargetEnvironmentsManager::class.java)
  }

  internal class TargetsList : BaseExtendableList<TargetEnvironmentConfiguration, RemoteTargetType<*>>(RemoteTargetType.EXTENSION_NAME) {
    override fun toBaseState(config: TargetEnvironmentConfiguration): OneTargetState =
      OneTargetState().also {
        it.loadFromConfiguration(config)
        it.runtimes = config.runtimes.state.configs
      }

    override fun fromOneState(state: BaseExtendableState): TargetEnvironmentConfiguration? {
      val result = super.fromOneState(state)
      if (result != null && state is OneTargetState) {
        result.runtimes.loadState(state.runtimes)
      }
      return result
    }

  }

  class TargetsListState : BaseState() {
    @get: XCollection(style = XCollection.Style.v2)
    var targets by list<OneTargetState>()
  }


  @Tag("target")
  class OneTargetState : BaseExtendableState() {
    @get: XCollection(style = XCollection.Style.v2)
    @get: Property(surroundWithTag = false)
    var runtimes by list<BaseExtendableState>()
  }
}