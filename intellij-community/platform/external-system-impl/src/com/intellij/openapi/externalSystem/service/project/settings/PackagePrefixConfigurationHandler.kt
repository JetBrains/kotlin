// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore


class PackagePrefixConfigurationHandler : ConfigurationHandler {
  override fun apply(module: Module, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    configuration.onPackagePrefixes {
      val sourceFolders = getAllSourceFolders(modelsProvider)
        .map { it to VfsUtilCore.urlToPath(it.url) }
      val sourceFolderManager = SourceFolderManager.getInstance(module.project)
      forEachPackagePrefix { path, packagePrefix ->
        val url = VfsUtilCore.pathToUrl(FileUtil.toCanonicalPath(path))
        sourceFolderManager.setSourceFolderPackagePrefix(url, packagePrefix)
        for ((sourceFolder, sourcePath) in sourceFolders) {
          if (!FileUtil.pathsEqual(path, sourcePath)) continue
          sourceFolder.packagePrefix = packagePrefix
        }
      }
    }
  }

  companion object {
    private fun getSourceFolders(module: Module, modelsProvider: IdeModifiableModelsProvider): List<SourceFolder> {
      val modifiableRootModel = modelsProvider.getModifiableRootModel(module)
      val contentEntries = modifiableRootModel.contentEntries
      return contentEntries.map { it.sourceFolders.toList() }.flatten()
    }

    private fun getAllSourceFolders(modelsProvider: IdeModifiableModelsProvider): List<SourceFolder> {
      return modelsProvider.modules.map { getSourceFolders(it, modelsProvider) }.flatten()
    }

    private fun ConfigurationData.onPackagePrefixes(action: Map<*, *>.() -> Unit) {
      val data = find("packagePrefix")
      if (data !is Map<*, *>) return
      data.action()
    }

    private fun Map<*, *>.forEachPackagePrefix(action: (String, String) -> Unit) {
      for ((sourcePath, packagePrefix) in this) {
        if (sourcePath !is String) {
          LOG.warn("unexpected value type: ${sourcePath?.javaClass?.name}, skipping")
          continue
        }
        if (packagePrefix !is String) {
          LOG.warn("unexpected value type: ${packagePrefix?.javaClass?.name}, skipping")
          continue
        }
        action(sourcePath, packagePrefix)
      }
    }
  }
}