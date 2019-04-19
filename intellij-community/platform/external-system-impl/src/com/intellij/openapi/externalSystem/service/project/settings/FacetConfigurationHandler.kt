// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.facet.Facet
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.FacetManager
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * Created by Nikita.Skvortsov
 * date: 12.09.2017.
 */
class FacetConfigurationHandler : ConfigurationHandler {
  override fun apply(module: Module, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    val modifiableModel = modelsProvider.getModifiableFacetModel(module)
    for (it in configuration.eachFacet { typeName, name, cfg ->
      handlerForType(typeName)?.process(module, name, cfg, FacetManager.getInstance(module))
    }) {
      modifiableModel.addFacet(it)
    }
  }

  override fun apply(project: Project, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {}

  private fun ConfigurationData.eachFacet(f: (String, String, Map<String, *>) -> Collection<Facet<out FacetConfiguration>>?): List<Facet<out FacetConfiguration>> {
    val runCfgMap = find("facets")

    if (runCfgMap !is List<*>) {
      return emptyList()
    }

    return runCfgMap.map { cfg ->
      if (cfg !is Map<*, *>) {
        LOG.warn("unexpected value type in facets map: ${cfg?.javaClass?.name}, skipping")
        return@map null
      }

      val name = cfg["name"]
      if (name !is String) {
        LOG.warn("unexpected key type in facets map: ${name?.javaClass?.name}, skipping")
        return@map null
      }

      val typeName = cfg["type"] as? String ?: name
      try {
        return@map f(typeName, name, cfg as Map<String, *>)
      }
      catch (e: Exception) {
        LOG.warn("Error occurred when importing run configuration ${name}: ${e.message}", e)
      }

      return@map null
    }
      .filterNotNull()
      .flatten()
  }
}

private fun handlerForType(typeName: String): FacetConfigurationImporter<out Facet<out FacetConfiguration>>? {
  return FacetConfigurationImporter.EP_NAME.extensionList.firstOrNull { it.canHandle(typeName) }
}