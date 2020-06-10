// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.serviceContainer.processAllImplementationClasses
import com.intellij.serviceContainer.processComponentInstancesOfType
import com.intellij.util.LineSeparator
import com.intellij.util.SmartList
import com.intellij.util.io.exists
import com.intellij.util.io.outputStream
import com.intellij.util.isEmpty
import com.intellij.util.write
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jdom.Element
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2

internal fun normalizeDefaultProjectElement(defaultProject: Project, element: Element, projectConfigDir: Path) {
  // first, process all known in advance components, because later all not known component names will be moved to misc.xml
  // (no way to get service stat spec because class cannot be loaded due to performance reasons)
  val iterator = element.getChildren("component").iterator()
  for (component in iterator) {
    when (val componentName = component.getAttributeValue("name")) {
      "InspectionProjectProfileManager" -> {
        iterator.remove()
        val schemeDir = projectConfigDir.resolve("inspectionProfiles")
        convertProfiles(component.getChildren("profile").iterator(), componentName, schemeDir)
        component.removeChild("version")
        writeProfileSettings(schemeDir, componentName, component)
      }

      "CopyrightManager" -> {
        iterator.remove()
        val schemeDir = projectConfigDir.resolve("copyright")
        convertProfiles(component.getChildren("copyright").iterator(), componentName, schemeDir)
        writeProfileSettings(schemeDir, componentName, component)
      }

      JpsProjectLoader.MODULE_MANAGER_COMPONENT -> {
        iterator.remove()
      }
    }
  }

  moveComponentConfiguration(defaultProject, element, { it }) { projectConfigDir.resolve(it) }
}

private fun writeProfileSettings(schemeDir: Path, componentName: String, component: Element) {
  component.removeAttribute("name")
  if (component.isEmpty()) {
    return
  }

  val wrapper = Element("component").setAttribute("name", componentName)
  component.name = "settings"
  wrapper.addContent(component)

  val file = schemeDir.resolve("profiles_settings.xml")
  file.outputStream().use {
    wrapper.write(it)
  }
}

private fun convertProfiles(profileIterator: MutableIterator<Element>, componentName: String, schemeDir: Path) {
  for (profile in profileIterator) {
    val schemeName = profile.getChildren("option").find { it.getAttributeValue("name") == "myName" }?.getAttributeValue("value") ?: continue

    profileIterator.remove()
    val wrapper = Element("component").setAttribute("name", componentName)
    wrapper.addContent(profile)
    val path = schemeDir.resolve("${FileUtil.sanitizeFileName(schemeName, true)}.xml")
    JDOMUtil.write(wrapper, path.outputStream(), "\n")
  }
}

internal fun moveComponentConfiguration(defaultProject: Project,
                                        element: Element,
                                        storagePathResolver: (storagePath: String) -> String,
                                        fileResolver: (name: String) -> Path) {
  val componentElements = element.getChildren("component")
  if (componentElements.isEmpty()) {
    return
  }

  val storageNameToComponentNames = HashMap<String, MutableSet<String>>()
  val workspaceComponentNames = HashSet(listOf("GradleLocalSettings"))
  val ignoredComponentNames = HashSet<String>()
  storageNameToComponentNames.put("workspace.xml", workspaceComponentNames)

  fun processComponents(aClass: Class<*>) {
    val stateAnnotation = getStateSpec(aClass) ?: return

    @Suppress("MoveVariableDeclarationIntoWhen")
    val storagePath = when {
      stateAnnotation.name.isEmpty() -> "misc.xml"
      else -> (stateAnnotation.storages.sortByDeprecated().firstOrNull() ?: return).path
    }

    when (storagePath) {
      StoragePathMacros.WORKSPACE_FILE -> workspaceComponentNames.add(stateAnnotation.name)
      StoragePathMacros.PRODUCT_WORKSPACE_FILE -> {
        // ignore - this data should be not copied
        ignoredComponentNames.add(stateAnnotation.name)
      }
      else -> storageNameToComponentNames.getOrPut(storagePathResolver(storagePath)) { ObjectOpenHashSet() }.add(stateAnnotation.name)
    }
  }

  processComponentInstancesOfType(defaultProject.picoContainer, PersistentStateComponent::class.java) {
    LOG.runAndLogException {
      processComponents(it.javaClass)
    }
  }

  processAllImplementationClasses(defaultProject.picoContainer) { aClass, _ ->
    processComponents(aClass)
    true
  }

  // fileResolver may return the same file for different storage names (e.g. for IPR project)
  val storagePathToComponentStates = HashMap<Path, MutableList<Element>>()
  val iterator = componentElements.iterator()
  cI@ for (componentElement in iterator) {
    iterator.remove()

    val name = componentElement.getAttributeValue("name") ?: continue
    if (ignoredComponentNames.contains(name)) {
      continue
    }

    for ((storageName, componentNames) in storageNameToComponentNames) {
      if (componentNames.contains(name)) {
        storagePathToComponentStates.getOrPut(fileResolver(storageName)) { SmartList() }.add(componentElement)
        continue@cI
      }
    }

    // ok, just save it to misc.xml
    storagePathToComponentStates.getOrPut(fileResolver("misc.xml")) { SmartList() }.add(componentElement)
  }

  for ((storageFile, componentStates) in storagePathToComponentStates) {
    writeConfigFile(componentStates, storageFile)
  }
}

private fun writeConfigFile(elements: List<Element>, file: Path) {
  if (elements.isEmpty()) {
    return
  }

  var wrapper = Element("project").setAttribute("version", "4")
  if (file.exists()) {
    try {
      wrapper = JDOMUtil.load(file)
    }
    catch (e: Exception) {
      LOG.warn(e)
    }
  }

  for (it in elements) {
    wrapper.addContent(it)
  }

  // .idea component configuration files uses XML prolog due to historical reasons
  file.outputStream().use {
    it.write(XML_PROLOG)
    it.write(LineSeparator.LF.separatorBytes)
    wrapper.write(it)
  }
}