// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.impl.ServiceManagerImpl
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.module.impl.ModuleManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.*
import com.intellij.util.containers.forEachGuaranteed
import com.intellij.util.io.exists
import com.intellij.util.io.outputStream
import gnu.trove.THashSet
import org.jdom.Element
import java.nio.file.FileSystems
import java.nio.file.Path

internal fun normalizeDefaultProjectElement(defaultProject: Project, element: Element, projectConfigDir: Path) {
  LOG.runAndLogException {
    moveComponentConfiguration(defaultProject, element) { projectConfigDir.resolve(it) }
  }

  val iterator = element.getChildren("component").iterator()
  for (component in iterator) {
    val componentName = component.getAttributeValue("name")

    fun writeProfileSettings(schemeDir: Path) {
      component.removeAttribute("name")
      if (component.isEmpty()) {
        return
      }

      val wrapper = Element("component").setAttribute("name", componentName)
      component.name = "settings"
      wrapper.addContent(component)

      val file = schemeDir.resolve("profiles_settings.xml")
      if (file.fileSystem == FileSystems.getDefault()) {
        // VFS must be used to write workspace.xml and misc.xml to ensure that project files will be not reloaded on external file change event
        writeFile(file, fakeSaveSession, null, createDataWriterForElement(wrapper, "default project"), LineSeparator.LF,
                  prependXmlProlog = false)
      }
      else {
        file.outputStream().use {
          wrapper.write(it)
        }
      }
    }

    when (componentName) {
      "InspectionProjectProfileManager" -> {
        iterator.remove()
        val schemeDir = projectConfigDir.resolve("inspectionProfiles")
        convertProfiles(component.getChildren("profile").iterator(), componentName, schemeDir)
        component.removeChild("version")
        writeProfileSettings(schemeDir)
      }

      "CopyrightManager" -> {
        iterator.remove()
        val schemeDir = projectConfigDir.resolve("copyright")
        convertProfiles(component.getChildren("copyright").iterator(), componentName, schemeDir)
        writeProfileSettings(schemeDir)
      }

      ModuleManagerImpl.COMPONENT_NAME -> {
        iterator.remove()
      }
    }
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

internal fun moveComponentConfiguration(defaultProject: Project, element: Element, fileResolver: (name: String) -> Path) {
  val componentElements = element.getChildren("component")
  if (componentElements.isEmpty()) {
    return
  }

  val workspaceComponentNames = THashSet(listOf("GradleLocalSettings"))
  val compilerComponentNames = THashSet<String>()

  fun processComponents(aClass: Class<*>) {
    val stateAnnotation = getStateSpec(aClass)
    if (stateAnnotation == null || stateAnnotation.name.isEmpty()) {
      return
    }

    val storage = stateAnnotation.storages.sortByDeprecated().firstOrNull() ?: return

    when {
      storage.path == StoragePathMacros.WORKSPACE_FILE -> workspaceComponentNames.add(stateAnnotation.name)
      storage.path == "compiler.xml" -> compilerComponentNames.add(stateAnnotation.name)
    }
  }

  @Suppress("DEPRECATION")
  val projectComponents = defaultProject.getComponents(PersistentStateComponent::class.java)
  projectComponents.forEachGuaranteed {
    processComponents(it.javaClass)
  }

  ServiceManagerImpl.processAllImplementationClasses(defaultProject) { aClass, _ ->
    processComponents(aClass)
    true
  }

  @Suppress("RemoveExplicitTypeArguments")
  val elements = mapOf(compilerComponentNames to SmartList<Element>(), workspaceComponentNames to SmartList<Element>())
  val iterator = componentElements.iterator()
  for (componentElement in iterator) {
    val name = componentElement.getAttributeValue("name") ?: continue
    for ((names, list) in elements) {
      if (names.contains(name)) {
        iterator.remove()
        list.add(componentElement)
      }
    }
  }

  for ((names, list) in elements) {
    writeConfigFile(list, fileResolver(if (names === workspaceComponentNames) "workspace.xml" else "compiler.xml"))
  }
}

private fun writeConfigFile(elements: List<Element>, file: Path) {
  if (elements.isEmpty()) {
    return
  }

  var wrapper = Element("project").setAttribute("version", "4")
  if (file.exists()) {
    try {
      wrapper = loadElement(file)
    }
    catch (e: Exception) {
      LOG.warn(e)
    }
  }
  elements.forEach { wrapper.addContent(it) }
  // .idea component configuration files uses XML prolog due to historical reasons
  if (file.fileSystem == FileSystems.getDefault()) {
    // VFS must be used to write workspace.xml and misc.xml to ensure that project files will be not reloaded on external file change event
    writeFile(file, fakeSaveSession, null, createDataWriterForElement(wrapper, "default project"), LineSeparator.LF, prependXmlProlog = true)
  }
  else {
    file.outputStream().use {
      it.write(XML_PROLOG)
      it.write(LineSeparator.LF.separatorBytes)
      wrapper.write(it)
    }
  }
}

private val fakeSaveSession = object : SaveSession {
  override fun save() {
  }
}