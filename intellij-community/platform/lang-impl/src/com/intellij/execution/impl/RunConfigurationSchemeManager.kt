// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.configurationStore.LazySchemeProcessor
import com.intellij.configurationStore.SchemeContentChangedHandler
import com.intellij.configurationStore.SchemeDataHolder
import com.intellij.execution.RunConfigurationConverter
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.UnknownConfigurationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.JDOMUtil
import gnu.trove.THashMap
import org.jdom.Element
import java.util.function.Function

private val LOG = logger<RunConfigurationSchemeManager>()

internal class RunConfigurationSchemeManager(private val manager: RunManagerImpl, private val templateDifferenceHelper: TemplateDifferenceHelper, private val isShared: Boolean, private val isWrapSchemeIntoComponentElement: Boolean) :
  LazySchemeProcessor<RunnerAndConfigurationSettingsImpl, RunnerAndConfigurationSettingsImpl>(), SchemeContentChangedHandler<RunnerAndConfigurationSettingsImpl> {

  private val converters by lazy {
    ConfigurationType.CONFIGURATION_TYPE_EP.extensionList.filterIsInstance(RunConfigurationConverter::class.java)
  }

  override fun getSchemeKey(scheme: RunnerAndConfigurationSettingsImpl): String {
    // here only isShared, because for workspace `workspaceSchemeManagerProvider.load` is used (see RunManagerImpl.loadState)
    return when {
      isShared -> {
        if (scheme.type.isManaged) {
          scheme.name
        }
        else {
          // do not use name as scheme key for Unknown RC or for Rider (some Rider RC types can use RC with not unique names)
          // using isManaged not strictly correct but separate API will be overkill for now
          scheme.uniqueID
        }
      }
      else -> "${scheme.type.id}-${scheme.name}"
    }
  }

  override fun createScheme(dataHolder: SchemeDataHolder<RunnerAndConfigurationSettingsImpl>, name: String, attributeProvider: Function<in String, String?>, isBundled: Boolean): RunnerAndConfigurationSettingsImpl {
    val settings = RunnerAndConfigurationSettingsImpl(manager)
    val element = readData(settings, dataHolder)
    manager.addConfiguration(element, settings, isCheckRecentsLimit = false)
    return settings
  }

  private fun readData(settings: RunnerAndConfigurationSettingsImpl, dataHolder: SchemeDataHolder<RunnerAndConfigurationSettingsImpl>): Element {
    var element = dataHolder.read()

    if (isShared && element.name == "component") {
      element = element.getChild("configuration") ?: throw RuntimeException("Unexpected element: " + JDOMUtil.write(element))
    }

    converters.any {
      LOG.runAndLogException { it.convertRunConfigurationOnDemand(element) } ?: false
    }

    try {
      settings.readExternal(element, isShared)
    }
    catch (e: InvalidDataException) {
      LOG.error(e)
    }

    var elementAfterStateLoaded: Element? = element

    if (!settings.needsToBeMigrated()) {
      try {
        elementAfterStateLoaded = writeScheme(settings)
      }
      catch (e: ProcessCanceledException) {
        throw e
      }
      catch (e: Throwable) {
        LOG.error("Cannot compute digest for RC using state after load", e)
      }
    }

    // very important to not write file with only changed line separators
    dataHolder.updateDigest(elementAfterStateLoaded)

    return element
  }

  override fun getSchemeKey(attributeProvider: Function<String, String?>, fileNameWithoutExtension: String): String? {
    var name = attributeProvider.apply("name")
    if (name == "<template>" || name == null) {
      attributeProvider.apply("type")?.let {
        if (name == null) {
          name = "<template>"
        }
        name += " of type ${it}"
      }
    }
    else if (name != null && !isShared) {
      val typeId = attributeProvider.apply("type")
      LOG.assertTrue(typeId != null)
      return "$typeId-${name}"
    }

    return name
  }

  override fun isExternalizable(scheme: RunnerAndConfigurationSettingsImpl) = true

  override fun schemeContentChanged(scheme: RunnerAndConfigurationSettingsImpl, name: String, dataHolder: SchemeDataHolder<RunnerAndConfigurationSettingsImpl>) {
    readData(scheme, dataHolder)
    manager.eventPublisher.runConfigurationChanged(scheme)
  }

  override fun onSchemeAdded(scheme: RunnerAndConfigurationSettingsImpl) {
    // createScheme automatically call addConfiguration
  }

  override fun onSchemeDeleted(scheme: RunnerAndConfigurationSettingsImpl) {
    manager.removeConfiguration(scheme)
  }

  override fun writeScheme(scheme: RunnerAndConfigurationSettingsImpl): Element? {
    val result = super.writeScheme(scheme) ?: return null
    if (isShared && isWrapSchemeIntoComponentElement) {
      return Element("component")
        .setAttribute("name", "ProjectRunConfigurationManager")
        .addContent(result)
    }
    else if (scheme.isTemplate) {
      val factory = scheme.factory
      if (factory != UnknownConfigurationType.getInstance() && !templateDifferenceHelper.isTemplateModified(result, factory)) {
        return null
      }
    }
    return result
  }
}

internal class TemplateDifferenceHelper(private val manager: RunManagerImpl) {
  private val cachedSerializedTemplateIdToData = THashMap<ConfigurationFactory, Element>()

  fun isTemplateModified(serialized: Element, factory: ConfigurationFactory): Boolean {
    val originalTemplate = cachedSerializedTemplateIdToData.getOrPut(factory) {
      JDOMUtil.internElement(manager.createTemplateSettings(factory).writeScheme())
    }
    return !JDOMUtil.areElementsEqual(serialized, originalTemplate)
  }

  fun clearCache() {
    cachedSerializedTemplateIdToData.clear()
  }
}