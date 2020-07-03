// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.ProjectTopics
import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.MultiMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.util.concurrent.Future

@State(name = "sourceFolderManager",  storages = [Storage(StoragePathMacros.CACHE_FILE)])
class SourceFolderManagerImpl(private val project: Project) : SourceFolderManager, Disposable, PersistentStateComponent<SourceFolderManagerState> {
  private val moduleNamesToSourceFolderState: MultiMap<String, SourceFolderModelState> = MultiMap.create()
  private var isDisposed = false
  private val mutex = Any()
  private var sourceFolders = PathPrefixTreeMap<SourceFolderModel>()
  private var sourceFoldersByModule = Object2ObjectOpenHashMap<String, ModuleModel>()
  @TestOnly
  @Volatile
  var bulkOperationState: Future<*>? = null

  override fun addSourceFolder(module: Module, url: String, type: JpsModuleSourceRootType<*>) {
    synchronized(mutex) {
      sourceFolders[url] = SourceFolderModel(module, url, type)
      addUrlToModuleModel(module, url)
    }
    ApplicationManager.getApplication().invokeLater(Runnable {
      VirtualFileManager.getInstance().refreshAndFindFileByUrl(url)
    }, project.disposed)
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

  private fun removeSourceFolder(url: String) {
    synchronized(mutex) {
      val sourceFolder = sourceFolders.remove(url) ?: return
      val module = sourceFolder.module
      val moduleModel = sourceFoldersByModule[module.name] ?: return
      val sourceFolders = moduleModel.sourceFolders
      sourceFolders.remove(url)
      if (sourceFolders.isEmpty()) {
        sourceFoldersByModule.remove(module.name)
      }
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
    val sourceFolders: MutableSet<String> = CollectionFactory.createFilePathSet()
  )

  init {
    project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: List<VFileEvent>) {
        val sourceFoldersToChange = HashMap<Module, ArrayList<Pair<VirtualFile, SourceFolderModel>>>()
        val virtualFileManager = VirtualFileManager.getInstance()

        for (event in events) {
          if (event !is VFileCreateEvent) {
            continue
          }
          val allDescendantValues = synchronized(mutex) { sourceFolders.getAllDescendantValues(VfsUtilCore.pathToUrl(event.path)) }
          for (sourceFolder in allDescendantValues) {
            val sourceFolderFile = virtualFileManager.refreshAndFindFileByUrl(sourceFolder.url)
            if (sourceFolderFile != null && sourceFolderFile.isValid) {
              sourceFoldersToChange.computeIfAbsent(sourceFolder.module) { ArrayList() }.add(Pair(event.file!!, sourceFolder))
              removeSourceFolder(sourceFolder.url)
            }
          }
        }

        bulkOperationState = ApplicationManager.getApplication().executeOnPooledThread { updateSourceFolders(sourceFoldersToChange) }
      }
    })

    project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
      override fun moduleAdded(project: Project, module: Module) {
        synchronized(mutex) {
          moduleNamesToSourceFolderState[module.name].forEach {
            loadSourceFolderState(it, module)
          }
          moduleNamesToSourceFolderState.remove(module.name)
        }
      }
    })
  }

  fun rescanAndUpdateSourceFolders() {
    val sourceFoldersToChange = HashMap<Module, ArrayList<Pair<VirtualFile, SourceFolderModel>>>()
    val virtualFileManager = VirtualFileManager.getInstance()

    val values = synchronized(mutex) { sourceFolders.values }
    for (sourceFolder in values) {
      val sourceFolderFile = virtualFileManager.refreshAndFindFileByUrl(sourceFolder.url)
      if (sourceFolderFile != null && sourceFolderFile.isValid) {
        sourceFoldersToChange.computeIfAbsent(sourceFolder.module) { ArrayList() }.add(Pair(sourceFolderFile, sourceFolder))
        removeSourceFolder(sourceFolder.url)
      }
    }
    updateSourceFolders(sourceFoldersToChange)
  }

  private fun updateSourceFolders(sourceFoldersToChange: Map<Module, List<Pair<VirtualFile, SourceFolderModel>>>) {
    for ((module, p) in sourceFoldersToChange) {
      ModuleRootModificationUtil.updateModel(module) { model ->
        for ((eventFile, sourceFolders) in p) {
          val (_, url, type, packagePrefix, generated) = sourceFolders
          val contentEntry = MarkRootActionBase.findContentEntry(model, eventFile)
                             ?: model.addContentEntry(url)
          val sourceFolder = contentEntry.addSourceFolder(url, type)
          if (packagePrefix != null && packagePrefix.isNotEmpty()) {
            sourceFolder.packagePrefix = packagePrefix
          }
          setForGeneratedSources(sourceFolder, generated)
        }
      }
    }
  }

  private fun setForGeneratedSources(folder: SourceFolder, generated: Boolean) {
    val jpsElement = folder.jpsElement
    val properties = jpsElement.getProperties(JavaModuleSourceRootTypes.SOURCES)
    if (properties != null) properties.isForGeneratedSources = generated
  }

  override fun getState(): SourceFolderManagerState? {
    synchronized(mutex) {
      return SourceFolderManagerState(sourceFolders.values.map { model ->
        val modelTypeName = dictionary.entries.find { it.value == model.type }?.key ?: return@map null
        SourceFolderModelState(model.module.name,
                               model.url,
                               modelTypeName,
                               model.packagePrefix,
                               model.generated)
      }.filterNotNull())
    }
  }

  override fun loadState(state: SourceFolderManagerState) {
    synchronized(mutex) {
      resetModuleAddedListeners()
      if (isDisposed) {
        return
      }
      sourceFolders = PathPrefixTreeMap()
      sourceFoldersByModule = Object2ObjectOpenHashMap()

      val moduleManager = ModuleManager.getInstance(project)

      state.sourceFolders.forEach { model ->
        val module = moduleManager.findModuleByName(model.moduleName)
        if (module == null) {
          listenToModuleAdded(model)
          return@forEach
        }
        loadSourceFolderState(model, module)
      }
    }
  }

  private fun resetModuleAddedListeners() = moduleNamesToSourceFolderState.clear()
  private fun listenToModuleAdded(model: SourceFolderModelState) = moduleNamesToSourceFolderState.putValue(model.moduleName, model)

  private fun loadSourceFolderState(model: SourceFolderModelState,
                                    module: Module) {
    val rootType: JpsModuleSourceRootType<*> = dictionary[model.type] ?: return
    val url = model.url
    sourceFolders[url] = SourceFolderModel(module, url, rootType, model.packagePrefix, model.generated)
    addUrlToModuleModel(module, url)
  }

  private fun addUrlToModuleModel(module: Module, url: String) {
    val moduleModel = sourceFoldersByModule.getOrPut(module.name) {
      ModuleModel(module).also {
        Disposer.register(module, Disposable {
          removeSourceFolders(module)
        })
      }
    }
    moduleModel.sourceFolders.add(url)
  }

  companion object {
    val dictionary = mapOf<String, JpsModuleSourceRootType<*>>(
      "SOURCE" to JavaSourceRootType.SOURCE,
      "TEST_SOURCE" to JavaSourceRootType.TEST_SOURCE,
      "RESOURCE" to JavaResourceRootType.RESOURCE,
      "TEST_RESOURCE" to JavaResourceRootType.TEST_RESOURCE
    )
  }
}

data class SourceFolderManagerState(var sourceFolders: Collection<SourceFolderModelState>) {
  constructor() : this(listOf<SourceFolderModelState>())
}

data class SourceFolderModelState(var moduleName: String,
                                  var url: String,
                                  var type: String,
                                  var packagePrefix: String?,
                                  var generated: Boolean) {
  constructor(): this("", "", "", null, false)
}
