// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isExternalStorageEnabled
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.roots.ProjectModelElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import gnu.trove.THashMap
import org.jdom.Element
import org.jetbrains.jps.model.serialization.java.compiler.JpsJavaCompilerConfigurationSerializer
import java.util.*

@State(name = "ExternalCompilerConfiguration", storages = [(Storage("compiler.xml"))], externalStorageOnly = true)
internal class ExternalCompilerConfigurationStorage(private val project: Project) : PersistentStateComponent<Element>, ProjectModelElement {
  var loadedState: Map<String, String>? = null
    private set

  override fun getState(): Element {
    val result = Element("state")
    if (!project.isExternalStorageEnabled) {
      return result
    }

    val map = (CompilerConfigurationImpl.getInstance(project) as CompilerConfigurationImpl).modulesBytecodeTargetMap
    val moduleNames = getFilteredModuleNameList(project, map, true)
    if (moduleNames.isNotEmpty()) {
      val element = Element(JpsJavaCompilerConfigurationSerializer.BYTECODE_TARGET_LEVEL)
      writeBytecodeTarget(moduleNames, map, element)
      result.addContent(element)
    }
    return result
  }

  override fun loadState(state: Element) {
    val result = THashMap<String, String>()
    readByteTargetLevel(state, result)
    loadedState = result
  }

  override fun getExternalSource(): ProjectModelExternalSource? {
    val externalProjectSystemRegistry = ExternalProjectSystemRegistry.getInstance()
    for (module in ModuleManager.getInstance(project).modules) {
      externalProjectSystemRegistry.getExternalSource(module)?.let {
        return it
      }
    }
    return null
  }
}

internal fun getFilteredModuleNameList(project: Project, map: Map<String, String>, isExternal: Boolean): List<String> {
  if (map.isEmpty()) {
    return emptyList()
  }

  if (!project.isExternalStorageEnabled) {
    return map.keys.toList()
  }

  val moduleManager = ModuleManager.getInstance(project)
  val externalProjectSystemRegistry = ExternalProjectSystemRegistry.getInstance()
  return map.keys.filter {
    // if no module and !isExternal - return true because CompilerConfigurationImpl saves module name as is without module existence check and this logic is preserved
    val module = moduleManager.findModuleByName(it) ?: return@filter !isExternal
    (externalProjectSystemRegistry.getExternalSource(module) != null) == isExternal
  }
}

internal fun writeBytecodeTarget(moduleNames: List<String>, map: Map<String, String>, element: Element) {
  Collections.sort(moduleNames, String.CASE_INSENSITIVE_ORDER)
  for (name in moduleNames) {
    val moduleElement = Element(JpsJavaCompilerConfigurationSerializer.MODULE)
    moduleElement.setAttribute(JpsJavaCompilerConfigurationSerializer.NAME, name)
    moduleElement.setAttribute(JpsJavaCompilerConfigurationSerializer.TARGET_ATTRIBUTE, map.get(name) ?: "")

    element.addContent(moduleElement)
  }
}

internal fun readByteTargetLevel(parentNode: Element, result: MutableMap<String, String>) {
  val bytecodeTargetElement = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.BYTECODE_TARGET_LEVEL) ?: return
  for (element in bytecodeTargetElement.getChildren(JpsJavaCompilerConfigurationSerializer.MODULE)) {
    val name = element.getAttributeValue(JpsJavaCompilerConfigurationSerializer.NAME) ?: continue
    val target = element.getAttributeValue(JpsJavaCompilerConfigurationSerializer.TARGET_ATTRIBUTE) ?: continue
    result.put(name, target)
  }
}