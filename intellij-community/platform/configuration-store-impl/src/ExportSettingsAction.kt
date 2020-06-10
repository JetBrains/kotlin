// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.AbstractBundle
import com.intellij.DynamicBundle
import com.intellij.configurationStore.schemeManager.ROOT_CONFIG
import com.intellij.configurationStore.schemeManager.SchemeManagerFactoryBase
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.ImportSettingsFilenameFilter
import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.*
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.options.SchemeManagerFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.util.io.FileUtil
import com.intellij.serviceContainer.ComponentManagerImpl
import com.intellij.serviceContainer.processAllImplementationClasses
import com.intellij.util.ArrayUtil
import com.intellij.util.ReflectionUtil
import com.intellij.util.containers.putValue
import com.intellij.util.io.*
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.io.IOException
import java.io.OutputStream
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

internal fun isImportExportActionApplicable(): Boolean {
  val app = ApplicationManager.getApplication()
  val storageManager = app.stateStore.storageManager as? StateStorageManagerImpl ?: return true
  return !storageManager.isStreamProviderPreventExportAction
}

// for Rider purpose
open class ExportSettingsAction : AnAction(), DumbAware {
  protected open fun getExportableComponents(): Map<Path, List<ExportableItem>> = getExportableComponentsMap(true, true)

  protected open fun exportSettings(saveFile: Path, markedComponents: Set<ExportableItem>) {
    val exportFiles = markedComponents.mapTo(ObjectOpenHashSet()) { it.file }
    saveFile.outputStream().use {
      exportSettings(exportFiles, it, FileUtil.toSystemIndependentName(PathManager.getConfigPath()))
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isImportExportActionApplicable()
  }

  override fun actionPerformed(e: AnActionEvent) {
    ApplicationManager.getApplication().saveSettings()

    val dialog = ChooseComponentsToExportDialog(getExportableComponents(), true,
                                                ConfigurationStoreBundle.message("title.select.components.to.export"),
                                                ConfigurationStoreBundle.message("prompt.please.check.all.components.to.export"))
    if (!dialog.showAndGet()) {
      return
    }

    val markedComponents = dialog.exportableComponents
    if (markedComponents.isEmpty()) {
      return
    }

    val saveFile = dialog.exportFile
    try {
      if (saveFile.exists() && showOkCancelDialog(
          title = IdeBundle.message("title.file.already.exists"),
          message = ConfigurationStoreBundle.message("prompt.overwrite.settings.file", saveFile.toString()),
          okText = IdeBundle.message("action.overwrite"),
          icon = Messages.getWarningIcon()) != Messages.OK) {
        return
      }

      exportSettings(saveFile, markedComponents)
      RevealFileAction.showDialog(getEventProject(e), ConfigurationStoreBundle.message("message.settings.exported.successfully"),
                                  ConfigurationStoreBundle.message("title.export.successful"), saveFile.toFile(), null)
    }
    catch (e: IOException) {
      Messages.showErrorDialog(ConfigurationStoreBundle.message("error.writing.settings", e.toString()), IdeBundle.message("title.error.writing.file"))
    }
  }
}

fun exportSettings(exportFiles: Set<Path>, out: OutputStream, configPath: String) {
  val filter = ObjectOpenHashSet<String>()
  Compressor.Zip(out).filter { entryName, _ -> filter.add(entryName) }.use { zip ->
    for (file in exportFiles) {
      val fileInfo = file.basicAttributesIfExists() ?: continue
      val relativePath = FileUtil.getRelativePath(configPath, file.toAbsolutePath().systemIndependentPath, '/')!!
      if (fileInfo.isDirectory) {
        zip.addDirectory(relativePath, file.toFile())
      }
      else {
        zip.addFile(relativePath, file.inputStream())
      }
    }

    exportInstalledPlugins(zip)

    zip.addFile(ImportSettingsFilenameFilter.SETTINGS_JAR_MARKER, ArrayUtil.EMPTY_BYTE_ARRAY)
  }
}

data class ExportableItem(val file: Path, val presentableName: String, val roamingType: RoamingType = RoamingType.DEFAULT)

fun exportInstalledPlugins(zip: Compressor) {
  val plugins = PluginManagerCore.getPlugins().asSequence().filter { !it.isBundled && it.isEnabled }.map { it.pluginId }.toList()
  if (plugins.isNotEmpty()) {
    val buffer = StringWriter()
    PluginManagerCore.writePluginsList(plugins, buffer)
    zip.addFile(PluginManager.INSTALLED_TXT, buffer.toString().toByteArray())
  }
}

// onlyPaths - include only specified paths (relative to config dir, ends with "/" if directory)
fun getExportableComponentsMap(isOnlyExisting: Boolean,
                               isComputePresentableNames: Boolean,
                               storageManager: StateStorageManager = ApplicationManager.getApplication().stateStore.storageManager,
                               onlyPaths: Set<String>? = null): Map<Path, List<ExportableItem>> {
  val result = LinkedHashMap<Path, MutableList<ExportableItem>>()
  @Suppress("DEPRECATION")
  val processor = { component: ExportableComponent ->
    for (file in component.exportFiles) {
      val item = ExportableItem(file.toPath(), component.presentableName, RoamingType.DEFAULT)
      result.putValue(item.file, item)
    }
  }

  val app = ApplicationManager.getApplication() as ComponentManagerImpl

  @Suppress("DEPRECATION")
  app.getComponentInstancesOfType(ExportableApplicationComponent::class.java).forEach(processor)
  @Suppress("DEPRECATION")
  ServiceBean.loadServicesFromBeans(ExportableComponent.EXTENSION_POINT, ExportableComponent::class.java).forEach(processor)

  val configPath = storageManager.expandMacros(ROOT_CONFIG)

  fun isSkipFile(file: Path): Boolean {
    if (onlyPaths != null) {
      var relativePath = FileUtil.getRelativePath(configPath, file.systemIndependentPath, '/')!!
      if (!file.fileName.toString().contains('.') && !file.isFile()) {
        relativePath += '/'
      }
      if (!onlyPaths.contains(relativePath)) {
        return true
      }
    }

    return isOnlyExisting && !file.exists()
  }

  if (isOnlyExisting || onlyPaths != null) {
    result.keys.removeAll(::isSkipFile)
  }

  val fileToContent = HashMap<Path, String>()

  processAllImplementationClasses(app.picoContainer) { aClass, pluginDescriptor ->
    val stateAnnotation = getStateSpec(aClass)
    @Suppress("DEPRECATION")
    if (stateAnnotation == null || stateAnnotation.name.isEmpty() || ExportableComponent::class.java.isAssignableFrom(aClass)) {
      return@processAllImplementationClasses true
    }

    val storage = stateAnnotation.storages.sortByDeprecated().firstOrNull() ?: return@processAllImplementationClasses true
    val isRoamable = getEffectiveRoamingType(storage.roamingType, storage.path) != RoamingType.DISABLED
    if (!isStorageExportable(storage, isRoamable)) {
      return@processAllImplementationClasses true
    }

    val additionalExportFile: Path?
    val file: Path

    try {
      additionalExportFile = getAdditionalExportFile(stateAnnotation, storageManager, ::isSkipFile)
      file = Paths.get(storageManager.expandMacros(storage.path))
    }
    catch (e: UnknownMacroException) {
      LOG.error("Cannot expand macro for component \"${stateAnnotation.name}\"", e)
      return@processAllImplementationClasses true
    }

    val isFileIncluded = !isSkipFile(file)
    if (isFileIncluded || additionalExportFile != null) {
      if (isComputePresentableNames && isOnlyExisting && additionalExportFile == null && file.fileName.toString().endsWith(".xml")) {
        val content = fileToContent.getOrPut(file) { file.readText() }
        if (!content.contains("""<component name="${stateAnnotation.name}"""")) {
          return@processAllImplementationClasses true
        }
      }

      val presentableName = if (isComputePresentableNames) getComponentPresentableName(stateAnnotation, aClass, pluginDescriptor) else ""
      if (isFileIncluded) {
        result.putValue(file, ExportableItem(file, presentableName, storage.roamingType))
      }
      if (additionalExportFile != null) {
        result.putValue(additionalExportFile, ExportableItem(additionalExportFile, "$presentableName (schemes)", RoamingType.DEFAULT))
      }
    }
    true
  }

  // must be in the end - because most of SchemeManager clients specify additionalExportFile in the State spec
  (SchemeManagerFactory.getInstance() as SchemeManagerFactoryBase).process {
    if (it.roamingType != RoamingType.DISABLED && it.fileSpec.getOrNull(0) != '$') {
      val file = Paths.get(storageManager.expandMacros(ROOT_CONFIG), it.fileSpec)
      if (!result.containsKey(file) && !isSkipFile(file)) {
        result.putValue(file, ExportableItem(file, it.presentableName ?: "", it.roamingType))
      }
    }
  }
  return result
}

private inline fun getAdditionalExportFile(stateAnnotation: State, storageManager: StateStorageManager, isSkipFile: (file: Path) -> Boolean): Path? {
  val additionalExportPath = stateAnnotation.additionalExportFile
  if (additionalExportPath.isEmpty()) {
    return null
  }

  val additionalExportFile: Path?
  // backward compatibility - path can contain macro
  if (additionalExportPath[0] == '$') {
    additionalExportFile = Paths.get(storageManager.expandMacros(additionalExportPath))
  }
  else {
    additionalExportFile = Paths.get(storageManager.expandMacros(ROOT_CONFIG), additionalExportPath)
  }
  return if (isSkipFile(additionalExportFile)) null else additionalExportFile
}

private fun isStorageExportable(storage: Storage, isRoamable: Boolean): Boolean =
  storage.exportable || isRoamable && storage.storageClass == StateStorage::class && storage.path.isNotEmpty()

private fun getComponentPresentableName(state: State, aClass: Class<*>, pluginDescriptor: PluginDescriptor?): String {
  val presentableName = state.presentableName.java
  if (presentableName != State.NameGetter::class.java) {
    try {
      return ReflectionUtil.newInstance(presentableName).get()
    }
    catch (e: Exception) {
      LOG.error(e)
    }
  }

  val defaultName = state.name

  fun trimDefaultName(): String {
    // Vcs.Log.App.Settings
    return defaultName
      .removeSuffix(".Settings")
      .removeSuffix(".Settings")
  }

  var resourceBundleName: String?
  if (pluginDescriptor != null && PluginManagerCore.CORE_ID != pluginDescriptor.pluginId) {
    resourceBundleName = pluginDescriptor.resourceBundleBaseName
    if (resourceBundleName == null) {
      if (pluginDescriptor.vendor == "JetBrains") {
        resourceBundleName = OptionsBundle.BUNDLE
      }
      else {
        return trimDefaultName()
      }
    }
  }
  else {
    resourceBundleName = OptionsBundle.BUNDLE
  }

  val classLoader = pluginDescriptor?.pluginClassLoader ?: aClass.classLoader
  if (classLoader != null) {
    val message = messageOrDefault(classLoader, resourceBundleName, defaultName)
    if (message !== defaultName) {
      return message
    }
  }
  return trimDefaultName()
}

private fun messageOrDefault(classLoader: ClassLoader, bundleName: String, defaultName: String): String {
  try {
    return AbstractBundle.messageOrDefault(
      DynamicBundle.INSTANCE.getResourceBundle(bundleName, classLoader), "exportable.$defaultName.presentable.name", defaultName)
  }
  catch (e: MissingResourceException) {
    LOG.warn("Missing bundle ${bundleName} at ${classLoader}: ${e.message}")
    return defaultName
  }
}