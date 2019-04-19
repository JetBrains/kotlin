// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.configurationStore

import com.intellij.configurationStore.DataWriter
import com.intellij.configurationStore.DataWriterFilter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.io.*
import org.jdom.Element
import java.nio.file.Path

private val LOG = logger<FileSystemExternalSystemStorage>()

internal interface ExternalSystemStorage {
  fun remove(name: String)

  fun read(name: String): Element?

  fun write(name: String, dataWriter: DataWriter?, filter: DataWriterFilter? = null)

  fun rename(oldName: String, newName: String)
}

internal class ModuleFileSystemExternalSystemStorage(project: Project) : FileSystemExternalSystemStorage("modules", project) {
  companion object {
    private fun nameToFilename(name: String) = "${sanitizeFileName(name)}.xml"
  }

  override fun nameToPath(name: String) = super.nameToPath(nameToFilename(name))
}

internal class ProjectFileSystemExternalSystemStorage(project: Project) : FileSystemExternalSystemStorage("project", project)

internal abstract class FileSystemExternalSystemStorage(dirName: String, project: Project) : ExternalSystemStorage {
  protected val dir: Path = ExternalProjectsDataStorage.getProjectConfigurationDir(project).resolve(dirName)

  var hasSomeData: Boolean
    private set

  init {
    val fileAttributes = dir.basicAttributesIfExists()
    hasSomeData = when {
      fileAttributes == null -> false
      fileAttributes.isRegularFile -> {
        // old binary format
        dir.parent.deleteChildrenStartingWith(dir.fileName.toString())
        false
      }
      else -> {
        LOG.assertTrue(fileAttributes.isDirectory)
        true
      }
    }
  }

  protected open fun nameToPath(name: String): Path = dir.resolve(name)

  override fun remove(name: String) {
    if (!hasSomeData) {
      return
    }

    nameToPath(name).delete()
  }

  override fun read(name: String): Element? {
    if (!hasSomeData) {
      return null
    }

    return nameToPath(name).inputStreamIfExists()?.use {
      JDOMUtil.load(it)
    }
  }

  override fun write(name: String, dataWriter: DataWriter?, filter: DataWriterFilter?) {
    if (dataWriter == null || (filter != null && !dataWriter.hasData(filter))) {
      remove(name)
      return
    }

    hasSomeData = true
    nameToPath(name).outputStream().use { dataWriter.write(it, filter = filter) }
  }

  override fun rename(oldName: String, newName: String) {
    if (!hasSomeData) {
      return
    }

    val oldFile = nameToPath(oldName)
    if (oldFile.exists()) {
      oldFile.move(nameToPath(newName))
    }
  }
}