// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.components.StateStorageChooserEx.Resolution
import com.intellij.openapi.roots.ProjectModelElement
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.PathUtilRt
import com.intellij.util.ReflectionUtil
import com.intellij.util.SmartList
import com.intellij.util.ThreeState
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.systemIndependentPath
import gnu.trove.THashMap
import org.jdom.Element
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import kotlin.concurrent.read
import kotlin.concurrent.write

private val MACRO_PATTERN = Pattern.compile("(\\$[^$]*\\$)")

/**
 * If componentManager not specified, storage will not add file tracker
 */
open class StateStorageManagerImpl(private val rootTagName: String,
                                   final override val macroSubstitutor: PathMacroSubstitutor? = null,
                                   override val componentManager: ComponentManager? = null,
                                   private val virtualFileTracker: StorageVirtualFileTracker? = createDefaultVirtualTracker(componentManager)) : StateStorageManager {
  private val macros: MutableList<Macro> = ContainerUtil.createLockFreeCopyOnWriteList()
  private val storageLock = ReentrantReadWriteLock()
  private val storages = THashMap<String, StateStorage>()

  val compoundStreamProvider: CompoundStreamProvider = CompoundStreamProvider()

  val isStreamProviderPreventExportAction: Boolean
    get() = compoundStreamProvider.providers.any { it.isDisableExportAction }

  override fun addStreamProvider(provider: StreamProvider, first: Boolean) {
    if (first) {
      compoundStreamProvider.providers.add(0, provider)
    }
    else {
      compoundStreamProvider.providers.add(provider)
    }
  }

  override fun removeStreamProvider(clazz: Class<out StreamProvider>) {
    compoundStreamProvider.providers.removeAll { clazz.isInstance(it) }
  }

  // access under storageLock
  @Suppress("LeakingThis")
  private var isUseVfsListener = when {
    componentManager == null || componentManager is Application -> ThreeState.NO
    else -> ThreeState.UNSURE // unsure because depends on stream provider state
  }

  open fun getFileBasedStorageConfiguration(fileSpec: String): FileBasedStorageConfiguration = defaultFileBasedStorageConfiguration

  protected open val isUseXmlProlog: Boolean
    get() = true

  companion object {
    private fun createDefaultVirtualTracker(componentManager: ComponentManager?): StorageVirtualFileTracker? {
      return when (componentManager) {
        null -> {
          null
        }
        is Application -> {
          StorageVirtualFileTracker(componentManager.messageBus)
        }
        else -> {
          val tracker = (ApplicationManager.getApplication().stateStore.storageManager as? StateStorageManagerImpl)?.virtualFileTracker
                        ?: return null
          Disposer.register(componentManager, Disposable {
            tracker.remove { it.storageManager.componentManager == componentManager }
          })
          tracker
        }
      }
    }
  }

  private data class Macro(val key: String, var value: String)

  @TestOnly
  fun getVirtualFileTracker() = virtualFileTracker

  /**
   * @param expansion System-independent
   */
  fun addMacro(key: String, expansion: String): Boolean {
    LOG.assertTrue(key.isNotEmpty())

    val value: String
    if (expansion.contains('\\')) {
      LOG.error("Macro $key set to system-dependent expansion $expansion")
      value = FileUtilRt.toSystemIndependentName(expansion)
    }
    else {
      value = expansion
    }

    // e.g ModuleImpl.setModuleFilePath update macro value
    for (macro in macros) {
      if (key == macro.key) {
        macro.value = value
        return false
      }
    }

    macros.add(Macro(key, value))
    return true
  }

  // system-independent paths
  open fun pathRenamed(oldPath: String, newPath: String, event: VFileEvent?) {
    for (macro in macros) {
      if (oldPath == macro.value) {
        macro.value = newPath
      }
    }
  }

  @Suppress("CAST_NEVER_SUCCEEDS")
  final override fun getStateStorage(storageSpec: Storage) = getOrCreateStorage(
    storageSpec.path,
    storageSpec.roamingType,
    storageSpec.storageClass.java,
    storageSpec.stateSplitter.java,
    storageSpec.exclusive,
    storageCreator = storageSpec as? StorageCreator
  )

  protected open fun normalizeFileSpec(fileSpec: String): String {
    val path = FileUtilRt.toSystemIndependentName(fileSpec)
    // fileSpec for directory based storage could be erroneously specified as "name/"
    return if (path.endsWith('/')) path.substring(0, path.length - 1) else path
  }

  // storageCustomizer - to ensure that other threads will use fully constructed and configured storage (invoked under the same lock as created)
  fun getOrCreateStorage(collapsedPath: String,
                         roamingType: RoamingType = RoamingType.DEFAULT,
                         storageClass: Class<out StateStorage> = StateStorage::class.java,
                         @Suppress("DEPRECATION") stateSplitter: Class<out StateSplitter> = StateSplitterEx::class.java,
                         exclusive: Boolean = false,
                         storageCustomizer: (StateStorage.() -> Unit)? = null,
                         storageCreator: StorageCreator? = null): StateStorage {
    val normalizedCollapsedPath = normalizeFileSpec(collapsedPath)
    val key: String
    if (storageClass == StateStorage::class.java) {
      if (normalizedCollapsedPath.isEmpty()) {
        throw Exception("Normalized path is empty, raw path '$collapsedPath'")
      }
      key = storageCreator?.key ?: normalizedCollapsedPath
    }
    else {
      key = storageClass.name!!
    }

    val storage = storageLock.read { storages.get(key) } ?: return storageLock.write {
      storages.getOrPut(key) {
        @Suppress("IfThenToElvis")
        val storage = if (storageCreator == null) createStateStorage(storageClass, normalizedCollapsedPath, roamingType, stateSplitter,
                                                                     exclusive)
        else storageCreator.create(this)
        storageCustomizer?.let { storage.it() }
        storage
      }
    }

    storageCustomizer?.let { storage.it() }
    return storage
  }

  fun getCachedFileStorages(): Set<StateStorage> = storageLock.read { storages.values.toSet() }

  fun findCachedFileStorage(name: String): StateStorage? = storageLock.read { storages.get(name) }

  fun getCachedFileStorages(changed: Collection<String>,
                            deleted: Collection<String>,
                            pathNormalizer: ((String) -> String)? = null): Pair<Collection<FileBasedStorage>, Collection<FileBasedStorage>> = storageLock.read {
    Pair(getCachedFileStorages(changed, pathNormalizer), getCachedFileStorages(deleted, pathNormalizer))
  }

  fun updatePath(spec: String, newPath: String) {
    val storage = getCachedFileStorages(listOf(spec)).firstOrNull() ?: return
    if (storage is StorageVirtualFileTracker.TrackedStorage) {
      virtualFileTracker?.let { tracker ->
        tracker.remove(storage.file.systemIndependentPath)
        tracker.put(newPath, storage)
      }
    }
    storage.setFile(null, resolvePath(newPath))
  }

  fun getCachedFileStorages(fileSpecs: Collection<String>, pathNormalizer: ((String) -> String)? = null): Collection<FileBasedStorage> {
    if (fileSpecs.isEmpty()) {
      return emptyList()
    }

    storageLock.read {
      var result: MutableList<FileBasedStorage>? = null
      for (fileSpec in fileSpecs) {
        val path = normalizeFileSpec(pathNormalizer?.invoke(fileSpec) ?: fileSpec)
        val storage = storages.get(path)
        if (storage is FileBasedStorage) {
          if (result == null) {
            result = SmartList<FileBasedStorage>()
          }
          result.add(storage)
        }
      }
      return result ?: emptyList()
    }
  }

  // overridden in upsource
  protected open fun createStateStorage(storageClass: Class<out StateStorage>,
                                        collapsedPath: String,
                                        roamingType: RoamingType,
                                        @Suppress("DEPRECATION") stateSplitter: Class<out StateSplitter>,
                                        exclusive: Boolean = false): StateStorage {
    if (storageClass != StateStorage::class.java) {
      val constructor = storageClass.constructors.first { it.parameterCount <= 3 }
      constructor.isAccessible = true
      if (constructor.parameterCount == 2) {
        return constructor.newInstance(componentManager!!, this) as StateStorage
      }
      else {
        return constructor.newInstance(collapsedPath, componentManager!!, this) as StateStorage
      }
    }

    val effectiveRoamingType = getEffectiveRoamingType(roamingType, collapsedPath)
    if (isUseVfsListener == ThreeState.UNSURE) {
      isUseVfsListener = ThreeState.fromBoolean(!compoundStreamProvider.isApplicable(collapsedPath, effectiveRoamingType))
    }

    val filePath = expandMacros(collapsedPath)
    @Suppress("DEPRECATION")
    if (stateSplitter != StateSplitter::class.java && stateSplitter != StateSplitterEx::class.java) {
      val storage = createDirectoryBasedStorage(filePath, collapsedPath, ReflectionUtil.newInstance(stateSplitter))
      if (storage is StorageVirtualFileTracker.TrackedStorage) {
        virtualFileTracker?.put(filePath, storage)
      }
      return storage
    }

    val app = ApplicationManager.getApplication()
    if (app != null && !app.isHeadlessEnvironment && PathUtilRt.getFileName(filePath).lastIndexOf('.') < 0) {
      throw IllegalArgumentException("Extension is missing for storage file: $filePath")
    }

    val storage = createFileBasedStorage(filePath, collapsedPath, effectiveRoamingType, if (exclusive) null else rootTagName)
    if (isUseVfsListener == ThreeState.YES && storage is StorageVirtualFileTracker.TrackedStorage) {
      virtualFileTracker?.put(filePath, storage)
    }
    return storage
  }

  // open for upsource
  protected open fun createFileBasedStorage(path: String,
                                            collapsedPath: String,
                                            roamingType: RoamingType,
                                            rootTagName: String?): StateStorage {
    val provider = if (roamingType == RoamingType.DISABLED) {
      // remove to ensure that repository doesn't store non-roamable files
      compoundStreamProvider.delete(collapsedPath, roamingType)
      null
    }
    else {
      compoundStreamProvider
    }
    return MyFileStorage(this, resolvePath(path), collapsedPath, rootTagName, roamingType, getMacroSubstitutor(collapsedPath), provider)
  }

  // open for upsource
  protected open fun createDirectoryBasedStorage(path: String, collapsedPath: String, @Suppress(
    "DEPRECATION") splitter: StateSplitter): StateStorage {
    return MyDirectoryStorage(this, resolvePath(path), splitter)
  }

  private class MyDirectoryStorage(override val storageManager: StateStorageManagerImpl, file: Path, @Suppress(
    "DEPRECATION") splitter: StateSplitter) :
    DirectoryBasedStorage(file, splitter, storageManager.macroSubstitutor), StorageVirtualFileTracker.TrackedStorage

  protected open class MyFileStorage(override val storageManager: StateStorageManagerImpl,
                                     file: Path,
                                     fileSpec: String,
                                     rootElementName: String?,
                                     roamingType: RoamingType,
                                     pathMacroManager: PathMacroSubstitutor? = null,
                                     provider: StreamProvider? = null) : FileBasedStorage(file, fileSpec, rootElementName, pathMacroManager,
                                                                                          roamingType,
                                                                                          provider), StorageVirtualFileTracker.TrackedStorage {
    override val isUseXmlProlog: Boolean
      get() = rootElementName != null && storageManager.isUseXmlProlog && !isSpecialStorage(fileSpec)

    override val configuration: FileBasedStorageConfiguration
      get() = storageManager.getFileBasedStorageConfiguration(fileSpec)

    override fun beforeElementSaved(elements: MutableList<Element>, rootAttributes: MutableMap<String, String>) {
      if (rootElementName != null) {
        storageManager.beforeElementSaved(elements, rootAttributes)
      }
      super.beforeElementSaved(elements, rootAttributes)
    }

    override fun beforeElementLoaded(element: Element) {
      storageManager.beforeElementLoaded(element)
      super.beforeElementLoaded(element)
    }

    override fun providerDataStateChanged(writer: DataWriter?, type: DataStateChanged) {
      storageManager.providerDataStateChanged(this, writer, type)
      super.providerDataStateChanged(writer, type)
    }

    override fun getResolution(component: PersistentStateComponent<*>, operation: StateStorageOperation): Resolution {
      if (operation == StateStorageOperation.WRITE && component is ProjectModelElement && storageManager.isExternalSystemStorageEnabled && component.externalSource != null) {
        return Resolution.CLEAR
      }
      return Resolution.DO
    }
  }

  open val isExternalSystemStorageEnabled: Boolean
    get() = false

  // function must be pure and do not use anything outside of passed arguments
  protected open fun beforeElementSaved(elements: MutableList<Element>, rootAttributes: MutableMap<String, String>) {
  }

  protected open fun providerDataStateChanged(storage: FileBasedStorage, writer: DataWriter?, type: DataStateChanged) {
  }

  protected open fun beforeElementLoaded(element: Element) {
  }

  final override fun rename(path: String, newName: String) {
    storageLock.write {
      val storage = getOrCreateStorage(collapseMacros(path), RoamingType.DEFAULT) as FileBasedStorage

      val file = storage.virtualFile
      try {
        if (file != null) {
          file.rename(storage, newName)
        }
        else if (storage.file.fileName.toString() != newName) {
          // old file didn't exist or renaming failed
          val expandedPath = expandMacros(path)
          val parentPath = PathUtilRt.getParentPath(expandedPath)
          storage.setFile(null, resolvePath(parentPath).resolve(newName))
          pathRenamed(expandedPath, "$parentPath/$newName", null)
        }
      }
      catch (e: IOException) {
        LOG.debug(e)
      }
    }
  }

  fun clearStorages() {
    storageLock.write {
      try {
        virtualFileTracker?.let {
          storages.forEachEntry { collapsedPath, _ ->
            it.remove(expandMacros(collapsedPath))
            true
          }
        }
      }
      finally {
        storages.clear()
      }
    }
  }

  protected open fun getMacroSubstitutor(fileSpec: String): PathMacroSubstitutor? = macroSubstitutor

  override fun expandMacros(path: String): String {
    // replacement can contains $ (php tests), so, this check must be performed before expand
    val matcher = MACRO_PATTERN.matcher(path)
    matcherLoop@
    while (matcher.find()) {
      val m = matcher.group(1)
      for ((key) in macros) {
        if (key == m) {
          continue@matcherLoop
        }
      }
      throw UnknownMacroException("Unknown macro: $m in storage file spec: $path")
    }

    var expanded = path
    for ((key, value) in macros) {
      expanded = StringUtil.replace(expanded, key, value)
    }
    return expanded
  }

  fun expandMacro(macro: String): String {
    for ((key, value) in macros) {
      if (key == macro) {
        return value
      }
    }

    throw UnknownMacroException("Unknown macro $macro")
  }

  fun collapseMacros(path: String): String {
    var result = path
    for ((key, value) in macros) {
      result = result.replace(value, key)
    }
    return normalizeFileSpec(result)
  }

  final override fun getOldStorage(component: Any, componentName: String, operation: StateStorageOperation): StateStorage? {
    val oldStorageSpec = getOldStorageSpec(component, componentName, operation) ?: return null
    return getOrCreateStorage(oldStorageSpec, RoamingType.DEFAULT)
  }

  protected open fun getOldStorageSpec(component: Any, componentName: String, operation: StateStorageOperation): String? = null

  protected open fun resolvePath(path: String): Path = Paths.get(path)
}

private fun String.startsWithMacro(macro: String): Boolean {
  val i = macro.length
  return getOrNull(i) == '/' && startsWith(macro)
}

fun removeMacroIfStartsWith(path: String, macro: String): String = if (path.startsWithMacro(macro)) path.substring(macro.length + 1) else path

@Suppress("DEPRECATION")
internal val Storage.path: String
  get() = if (value.isEmpty()) file else value

internal fun getEffectiveRoamingType(roamingType: RoamingType, collapsedPath: String): RoamingType {
  if (roamingType != RoamingType.DISABLED && (collapsedPath == StoragePathMacros.WORKSPACE_FILE || collapsedPath == StoragePathMacros.NON_ROAMABLE_FILE || isSpecialStorage(collapsedPath))) {
    return RoamingType.DISABLED
  }
  else {
    return roamingType
  }
}

class UnknownMacroException(message: String) : RuntimeException(message)