// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManagerImpl
import com.intellij.openapi.vfs.encoding.EncodingProjectManagerImpl.*
import java.nio.charset.Charset

class EncodingConfigurationHandler : ConfigurationHandler {
  override fun onSuccessImport(project: Project,
                               projectData: ProjectData?,
                               modelsProvider: IdeModelsProvider,
                               configuration: ConfigurationData) {
    configuration.onEncodingBlock {
      val encodingManager = EncodingProjectManager.getInstance(project) as EncodingProjectManagerImpl
      val mapping = getCharsetProjectMapping(encodingManager)
      TransactionGuard.getInstance().submitTransactionAndWait {
        encodingManager.setMapping(mapping)
        onCharset("encoding") {
          encodingManager.defaultCharsetName = it?.name() ?: ""
        }
        onBomPolicy("bomPolicy") {
          encodingManager.setBOMForNewUtf8Files(it)
        }
        onMap("properties") {
          onCharset("encoding") {
            encodingManager.setDefaultCharsetForPropertiesFiles(null, it)
          }
          onBoolean("transparentNativeToAsciiConversion") {
            encodingManager.setNative2AsciiForPropertiesFiles(null, it)
          }
        }
      }
    }
  }

  companion object {
    private const val DEFAULT_CHARSET = "<System Default>"

    private fun ConfigurationData.onEncodingBlock(action: Map<*, *>.() -> Unit) {
      val data = find("encodings")
      if (data !is Map<*, *>) return
      data.action()
    }

    private inline fun <reified T> Map<*, *>.on(name: String, action: (T) -> Unit) {
      val data = get(name) ?: return
      if (data !is T) {
        LOG.warn("unexpected type ${data.javaClass.name} of $name encoding configuration, skipping")
        return
      }
      action(data)
    }

    private fun Map<*, *>.onBomPolicy(name: String, action: (BOMForNewUTF8Files) -> Unit) = on<String>(name) {
      val option = when (it) {
        "WITH_BOM" -> BOMForNewUTF8Files.ALWAYS
        "WITH_NO_BOM" -> BOMForNewUTF8Files.NEVER
        "WITH_BOM_ON_WINDOWS" -> BOMForNewUTF8Files.WINDOWS_ONLY
        else -> {
          LOG.warn("unsupported BOM policy $it of encoding configuration, skipping")
          return@on
        }
      }
      action(option)
    }

    private fun Map<*, *>.onCharset(name: String, action: (Charset?) -> Unit) = on<String>(name) {
      if (it == DEFAULT_CHARSET) {
        action(null)
        return@on
      }
      val charset = CharsetToolkit.forName(it)
      if (charset == null) {
        LOG.warn("unsupported charset $it of $name encoding configuration, skipping")
        return@on
      }
      action(charset)
    }

    private fun Map<*, *>.onMap(name: String, action: Map<*, *>.() -> Unit) = on(name, action)

    private fun Map<*, *>.onBoolean(name: String, action: (Boolean) -> Unit) = on(name, action)

    private fun Map<*, *>.forEachCharsetMapping(action: (String, String) -> Unit) = onMap("mapping") {
      for ((path, charsetName) in this) {
        if (path !is String) {
          LOG.warn("unexpected path type ${path?.javaClass?.name}, skipping")
          continue
        }
        if (charsetName !is String) {
          LOG.warn("unexpected type ${charsetName?.javaClass?.name} of $path encoding configuration, skipping")
          continue
        }
        action(path, charsetName)
      }
    }

    private fun Map<*, *>.getCharsetProjectMapping(encodingManager: EncodingProjectManagerImpl): Map<VirtualFile, Charset> {
      val mapping = encodingManager.allMappings.toMutableMap()
      forEachCharsetMapping { path, charsetName ->
        val url = VfsUtilCore.pathToUrl(FileUtil.toCanonicalPath(path))
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFile = virtualFileManager.findFileByUrl(url)
        if (virtualFile == null) {
          LOG.warn("mappings file $path not found, skipping")
          return@forEachCharsetMapping
        }
        if (charsetName == DEFAULT_CHARSET) {
          mapping.remove(virtualFile)
          return@forEachCharsetMapping
        }
        val charset = CharsetToolkit.forName(charsetName)
        if (charset == null) {
          LOG.warn("unsupported charset $charsetName of $path encoding configuration, skipping")
          return@forEachCharsetMapping
        }
        mapping[virtualFile] = charset
      }
      return mapping
    }
  }
}