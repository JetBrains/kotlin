// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.configurationStore.schemeManager.createDir
import com.intellij.configurationStore.schemeManager.getOrCreateChild
import com.intellij.openapi.components.PathMacroSubstitutor
import com.intellij.openapi.components.StateSplitterEx
import com.intellij.openapi.components.impl.stores.DirectoryStorageUtil
import com.intellij.openapi.components.impl.stores.FileStorageCoreUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.LineSeparator
import com.intellij.util.SmartList
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.isEmpty
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Path

abstract class DirectoryBasedStorageBase(@Suppress("DEPRECATION") protected val splitter: com.intellij.openapi.components.StateSplitter,
                                         protected val pathMacroSubstitutor: PathMacroSubstitutor? = null) : StateStorageBase<StateMap>() {
  protected var componentName: String? = null

  protected abstract val virtualFile: VirtualFile?

  public override fun loadData(): StateMap = StateMap.fromMap(DirectoryStorageUtil.loadFrom(virtualFile, pathMacroSubstitutor))

  override fun createSaveSessionProducer(): SaveSessionProducer? = null

  override fun analyzeExternalChangesAndUpdateIfNeeded(componentNames: MutableSet<String>) {
    // todo reload only changed file, compute diff
    val newData = loadData()
    storageDataRef.set(newData)
    if (componentName != null) {
      componentNames.add(componentName!!)
    }
  }

  override fun getSerializedState(storageData: StateMap, component: Any?, componentName: String, archive: Boolean): Element? {
    this.componentName = componentName

    if (storageData.isEmpty()) {
      return null
    }

    // FileStorageCoreUtil on load check both component and name attributes (critical important for external store case, where we have only in-project artifacts, but not external)
    val state = Element(FileStorageCoreUtil.COMPONENT).setAttribute(FileStorageCoreUtil.NAME, componentName)
    if (splitter is StateSplitterEx) {
      for (fileName in storageData.keys()) {
        val subState = storageData.getState(fileName, archive) ?: return null
        splitter.mergeStateInto(state, subState.clone())
      }
    }
    else {
      val subElements = SmartList<Element>()
      for (fileName in storageData.keys()) {
        val subState = storageData.getState(fileName, archive) ?: return null
        subElements.add(subState.clone())
      }

      if (!subElements.isEmpty()) {
        splitter.mergeStatesInto(state, subElements.toTypedArray())
      }
    }
    return state
  }

  override fun hasState(storageData: StateMap, componentName: String): Boolean = storageData.hasStates()
}

@ApiStatus.Internal
interface DirectoryBasedSaveSessionProducer : SaveSessionProducer {
  fun setFileState(fileName: String, componentName: String, element: Element?)
}

open class DirectoryBasedStorage(private val dir: Path,
                                 @Suppress("DEPRECATION") splitter: com.intellij.openapi.components.StateSplitter,
                                 pathMacroSubstitutor: PathMacroSubstitutor? = null) : DirectoryBasedStorageBase(splitter, pathMacroSubstitutor) {
  override val isUseVfsForWrite: Boolean
    get() = true

  @Volatile
  private var cachedVirtualFile: VirtualFile? = null

  override val virtualFile: VirtualFile?
    get() {
      var result = cachedVirtualFile
      if (result == null) {
        result = LocalFileSystem.getInstance().findFileByPath(dir.systemIndependentPath)
        cachedVirtualFile = result
      }
      return result
    }

  internal fun setVirtualDir(dir: VirtualFile?) {
    cachedVirtualFile = dir
  }

  override fun createSaveSessionProducer(): SaveSessionProducer? = if (checkIsSavingDisabled()) null else MySaveSession(this, getStorageData())

  private class MySaveSession(private val storage: DirectoryBasedStorage, private val originalStates: StateMap) : SaveSessionBase(), SaveSession, DirectoryBasedSaveSessionProducer {
    private var copiedStorageData: MutableMap<String, Any>? = null

    private val dirtyFileNames = HashSet<String>()
    private var isSomeFileRemoved = false

    override fun setSerializedState(componentName: String, element: Element?) {
      storage.componentName = componentName

      val stateAndFileNameList = if (element.isEmpty()) emptyList() else storage.splitter.splitState(element!!)
      if (stateAndFileNameList.isEmpty()) {
        if (copiedStorageData != null) {
          copiedStorageData!!.clear()
        }
        else if (!originalStates.isEmpty()) {
          copiedStorageData = HashMap()
        }
        return
      }

      val existingFiles = ObjectOpenHashSet<String>(stateAndFileNameList.size)
      for (pair in stateAndFileNameList) {
        doSetState(pair.second, pair.first)
        existingFiles.add(pair.second)
      }

      for (key in originalStates.keys()) {
        if (existingFiles.contains(key)) {
          continue
        }
        removeFileData(key)
      }
    }

    override fun setFileState(fileName: String, componentName: String, element: Element?) {
      storage.componentName = componentName
      if (element != null) {
        doSetState(fileName, element)
      }
      else {
        removeFileData(fileName)
      }
    }

    private fun removeFileData(fileName: String) {
      if (copiedStorageData == null) {
        copiedStorageData = originalStates.toMutableMap()
      }
      isSomeFileRemoved = true
      copiedStorageData!!.remove(fileName)
    }

    private fun doSetState(fileName: String, subState: Element) {
      if (copiedStorageData == null) {
        copiedStorageData = setStateAndCloneIfNeeded(fileName, subState, originalStates)
        if (copiedStorageData != null) {
          dirtyFileNames.add(fileName)
        }
      }
      else if (updateState(copiedStorageData!!, fileName, subState)) {
        dirtyFileNames.add(fileName)
      }
    }

    override fun createSaveSession() = if (storage.checkIsSavingDisabled() || copiedStorageData == null) null else this

    override fun save() {
      val stateMap = StateMap.fromMap(copiedStorageData!!)

      if (copiedStorageData!!.isEmpty()) {
        val dir = storage.virtualFile
        if (dir != null && dir.exists()) {
          dir.delete(this)
        }
        storage.setStorageData(stateMap)
        return
      }

      if (dirtyFileNames.isNotEmpty()) {
        saveStates(stateMap)
      }
      if (isSomeFileRemoved) {
        val dir = storage.virtualFile
        if (dir != null && dir.exists()) {
          deleteFiles(dir)
        }
      }

      storage.setStorageData(stateMap)
    }

    private fun saveStates(states: StateMap) {
      var dir = storage.cachedVirtualFile
      for (fileName in states.keys()) {
        if (!dirtyFileNames.contains(fileName)) {
          continue
        }

        val element = states.getElement(fileName) ?: continue

        if (dir == null || !dir.exists()) {
          dir = storage.virtualFile
          if (dir == null || !dir.exists()) {
            dir = createDir(storage.dir, this)
            storage.cachedVirtualFile = dir
          }
        }

        try {
          val file = dir.getOrCreateChild(fileName, this)
          // we don't write xml prolog due to historical reasons (and should not in any case)
          val macroManager = if (storage.pathMacroSubstitutor == null) null else (storage.pathMacroSubstitutor as TrackingPathMacroSubstitutorImpl).macroManager
          val xmlDataWriter = XmlDataWriter(FileStorageCoreUtil.COMPONENT, listOf(element), mapOf(FileStorageCoreUtil.NAME to storage.componentName!!), macroManager, dir.path)
          writeFile(null, this, file, xmlDataWriter, getOrDetectLineSeparator(file) ?: LineSeparator.getSystemLineSeparator(), false)
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }
    }

    private fun deleteFiles(dir: VirtualFile) {
      val copiedStorageData = copiedStorageData!!
      for (file in dir.children) {
        val fileName = file.name
        if (fileName.endsWith(FileStorageCoreUtil.DEFAULT_EXT) && !copiedStorageData.containsKey(fileName)) {
          if (file.isWritable) {
            file.delete(this)
          }
          else {
            throw ReadOnlyModificationException(file, null)
          }
        }
      }
    }
  }

  private fun setStorageData(newStates: StateMap) {
    storageDataRef.set(newStates)
  }

  override fun toString() = "${javaClass.simpleName}(file=${virtualFile?.path}, componentName=$componentName)"
}

private fun getOrDetectLineSeparator(file: VirtualFile): LineSeparator? {
  if (!file.exists()) {
    return null
  }

  file.detectedLineSeparator?.let {
    return LineSeparator.fromString(it)
  }
  val lineSeparator = detectLineSeparators(Charsets.UTF_8.decode(ByteBuffer.wrap(file.contentsToByteArray())))
  file.detectedLineSeparator = lineSeparator.separatorString
  return lineSeparator
}