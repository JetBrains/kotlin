// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

class SourceFolderManagerImpl(private val project: Project) : SourceFolderManager, Disposable {

  private var isDisposed = false
  private val mutex = Any()
  private val sourceFolders = PathPrefixTreeMap<SourceFolderModel>()
  private val sourceFoldersByModule = THashMap<String, ModuleModel>()

  override fun addSourceFolder(module: Module, url: String, type: JpsModuleSourceRootType<*>) {
    synchronized(mutex) {
      sourceFolders[url] = SourceFolderModel(module, url, type)
      val moduleModel = sourceFoldersByModule.getOrPut(module.name) {
        ModuleModel(module).also {
          Disposer.register(module, Disposable {
            removeSourceFolders(module)
          })
        }
      }
      moduleModel.sourceFolders.add(url)
    }
    TransactionGuard.getInstance().submitTransactionLater(this, Runnable {
      val virtualFileManager = VirtualFileManager.getInstance()
      virtualFileManager.refreshAndFindFileByUrl(url)
    })
  }

  override fun setSourceFolderPackagePrefix(url: String, packagePrefix: String?) {
    synchronized(mutex) {
      val sourceFolder = sourceFolders[url] ?: return
      sourceFolder.packagePrefix = packagePrefix
    }
  }

  override fun setSourceFolderGenerated(url: String, generated: Boolean) {
    synchronized(mutex) {
      val sourceFolder = sourceFolders[url] ?: return
      sourceFolder.generated = generated
    }
  }

  override fun removeSourceFolders(module: Module) {
    synchronized(mutex) {
      val moduleModel = sourceFoldersByModule.remove(module.name) ?: return
      moduleModel.sourceFolders.forEach { sourceFolders.remove(it) }
    }
  }

  override fun dispose() {
    assert(!isDisposed) { "Source folder manager already disposed" }
    isDisposed = true
  }

  @TestOnly
  fun isDisposed() = isDisposed

  @TestOnly
  fun getSourceFolders(moduleName: String) = synchronized(mutex) {
    sourceFoldersByModule[moduleName]?.sourceFolders
  }

  private fun unsafeRemoveSourceFolder(url: String) {
    val sourceFolder = sourceFolders.remove(url) ?: return
    val module = sourceFolder.module
    val moduleModel = sourceFoldersByModule[module.name] ?: return
    val sourceFolders = moduleModel.sourceFolders
    sourceFolders.remove(url)
    if (sourceFolders.isEmpty()) {
      sourceFoldersByModule.remove(module.name)
    }
  }

  private data class SourceFolderModel(
    val module: Module,
    val url: String,
    val type: JpsModuleSourceRootType<*>,
    var packagePrefix: String? = null,
    var generated: Boolean = false
  )

  private data class ModuleModel(
    val module: Module,
    val sourceFolders: MutableSet<String> = THashSet(FileUtil.PATH_HASHING_STRATEGY)
  )

  init {
    project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: List<VFileEvent>) {
        val sourceFoldersToChange = HashMap<Module, ArrayList<Pair<VirtualFile, SourceFolderModel>>>()
        val virtualFileManager = VirtualFileManager.getInstance()
        synchronized(mutex) {
          for (event in events) {
            if (event !is VFileCreateEvent) {
              continue
            }

            for (sourceFolder in sourceFolders.getAllDescendantValues(VfsUtilCore.pathToUrl(event.path))) {
              val sourceFolderFile = virtualFileManager.refreshAndFindFileByUrl(sourceFolder.url)
              if (sourceFolderFile != null && sourceFolderFile.isValid) {
                sourceFoldersToChange.computeIfAbsent(sourceFolder.module) { ArrayList() }.add(Pair(event.file!!, sourceFolder))
                unsafeRemoveSourceFolder(sourceFolder.url)
              }
            }
          }

          for ((module, p) in sourceFoldersToChange) {
            ModuleRootModificationUtil.updateModel(module) { model ->
              for ((eventFile, sourceFolders) in p) {
                val (_, url, type, packagePrefix, generated) = sourceFolders
                val contentEntry = MarkRootActionBase.findContentEntry(model, eventFile)
                if (contentEntry != null) {
                  val sourceFolder = contentEntry.addSourceFolder(url, type)
                  if (packagePrefix != null && packagePrefix.isNotEmpty()) {
                    sourceFolder.packagePrefix = packagePrefix
                  }
                  setForGeneratedSources(sourceFolder, generated)
                }
              }
            }
          }
        }
      }
    })
  }
  private fun setForGeneratedSources(folder: SourceFolder, generated: Boolean) {
    val jpsElement = folder.jpsElement
    val properties = jpsElement.getProperties(JavaModuleSourceRootTypes.SOURCES)
    if (properties != null) properties.isForGeneratedSources = generated
  }
}