// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.configurationStore.ComponentSerializationUtil
import com.intellij.execution.remote.BaseExtendableConfiguration.Companion.getTypeImpl
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.xmlb.annotations.XCollection

open class BaseExtendableList<C, T>(private val extPoint: ExtensionPointName<T>)
  : PersistentStateComponent<BaseExtendableList.ListState>
  where C : BaseExtendableConfiguration, T : BaseExtendableType<out C> {

  private val resolvedInstances = mutableListOf<C>()
  private val unresolvedInstances = mutableListOf<BaseExtendableState>()

  fun clear() {
    resolvedInstances.clear()
    unresolvedInstances.clear()
  }

  fun findConfig(name: String): C? {
    for (resolvedInstance in resolvedInstances) {
      if (resolvedInstance.displayName == name) {
        return resolvedInstance
      }
    }
    return null
  }

  fun resolvedConfigs(): List<C> = resolvedInstances.toList()

  fun addConfig(config: C) = resolvedInstances.add(config)

  fun removeConfig(config: C) = resolvedInstances.remove(config)

  override fun getState(): ListState = ListState().also {
    it.configs.addAll(resolvedInstances.map { toBaseState(it) })
    it.configs.addAll(unresolvedInstances)
  }

  override fun loadState(state: ListState) {
    loadState(state.configs)
  }

  fun loadState(configs: List<BaseExtendableState>) {
    clear()
    configs.forEach {
      val nextConfig = fromOneState(it)
      if (nextConfig == null) {
        unresolvedInstances.add(it)
      }
      else {
        resolvedInstances.add(nextConfig)
      }
    }
  }

  open fun toBaseState(config: C): BaseExtendableState = BaseExtendableState().also {
    it.loadFromConfiguration(config)
  }

  protected open fun fromOneState(state: BaseExtendableState): C? {
    val type = extPoint.extensionList.firstOrNull { it.id == state.typeId }
    val defaultConfig = type?.createDefaultConfig()
    return defaultConfig?.also {
      it.displayName = state.name ?: ""
      ComponentSerializationUtil.loadComponentState(it.getSerializer(), state.innerState)
    }
  }

  companion object {
    private fun BaseExtendableConfiguration.getSerializer() = getTypeImpl().createSerializer(this)
  }

  open class ListState : BaseState() {
    @get: XCollection(style = XCollection.Style.v2)
    var configs by list<BaseExtendableState>()
  }
}