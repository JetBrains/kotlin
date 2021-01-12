// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.configurationStore.*
import com.intellij.ide.ui.UITheme
import com.intellij.ide.ui.laf.TempUIThemeBasedLookAndFeelInfo
import com.intellij.openapi.application.ex.DecodeDefaultsUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.impl.stores.FileStorageCoreUtil
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.options.SchemeProcessor
import com.intellij.openapi.options.SchemeState
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.SafeWriteRequestor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.util.*
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.catch
import com.intellij.util.containers.mapSmart
import com.intellij.util.io.*
import com.intellij.util.text.UniqueNameGenerator
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jdom.Document
import org.jdom.Element
import java.io.File
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import java.util.function.Predicate

class SchemeManagerImpl<T : Any, MUTABLE_SCHEME : T>(val fileSpec: String,
                                                     processor: SchemeProcessor<T, MUTABLE_SCHEME>,
                                                     private val provider: StreamProvider?,
                                                     internal val ioDirectory: Path,
                                                     val roamingType: RoamingType = RoamingType.DEFAULT,
                                                     val presentableName: String? = null,
                                                     private val schemeNameToFileName: SchemeNameToFileName = CURRENT_NAME_CONVERTER,
                                                     private val fileChangeSubscriber: FileChangeSubscriber? = null,
                                                     private val virtualFileResolver: VirtualFileResolver? = null) : SchemeManagerBase<T, MUTABLE_SCHEME>(processor), SafeWriteRequestor, StorageManagerFileWriteRequestor {
  private val isUseVfs: Boolean
    get() = fileChangeSubscriber != null || virtualFileResolver != null

  internal val isOldSchemeNaming = schemeNameToFileName == OLD_NAME_CONVERTER

  private val isLoadingSchemes = AtomicBoolean()

  internal val schemeListManager = SchemeListManager(this)

  internal val schemes: MutableList<T>
    get() = schemeListManager.schemes

  internal var cachedVirtualDirectory: VirtualFile? = null

  internal val schemeExtension: String
  private val updateExtension: Boolean

  internal val filesToDelete = ContainerUtil.newConcurrentSet<String>()

  // scheme could be changed - so, hashcode will be changed - we must use identity hashing strategy
  internal val schemeToInfo = ConcurrentCollectionFactory.createConcurrentIdentityMap<T, ExternalInfo>()

  init {
    if (processor is SchemeExtensionProvider) {
      schemeExtension = processor.schemeExtension
      updateExtension = true
    }
    else {
      schemeExtension = FileStorageCoreUtil.DEFAULT_EXT
      updateExtension = false
    }

    if (isUseVfs) {
      LOG.runAndLogException { refreshVirtualDirectory() }
    }
  }

  override val rootDirectory: File
    get() = ioDirectory.toFile()

  override val allSchemeNames: Collection<String>
    get() = schemes.mapSmart { processor.getSchemeKey(it) }

  override val allSchemes: List<T>
    get() = Collections.unmodifiableList(schemes)

  override val isEmpty: Boolean
    get() = schemes.isEmpty()

  private fun refreshVirtualDirectory() {
    // store refreshes root directory, so, we don't need to use refreshAndFindFile
    val directory = LocalFileSystem.getInstance().findFileByPath(ioDirectory.systemIndependentPath) ?: return
    cachedVirtualDirectory = directory
    directory.children
    if (directory is NewVirtualFile) {
      directory.markDirty()
    }

    directory.refresh(true, false)
  }

  override fun loadBundledScheme(resourceName: String, requestor: Any) {
    try {
      @Suppress("DEPRECATION")
      val url = when (requestor) {
        is com.intellij.openapi.extensions.AbstractExtensionPointBean -> requestor.loaderForClass.getResource(resourceName)
        is TempUIThemeBasedLookAndFeelInfo -> File(resourceName).toURI().toURL()
        is UITheme -> DecodeDefaultsUtil.getDefaults(requestor.providerClassLoader, resourceName)
        else -> DecodeDefaultsUtil.getDefaults(requestor, resourceName)
      }

      if (url == null) {
        LOG.error("Cannot read scheme from $resourceName")
        return
      }

      val bytes = URLUtil.openStream(url).readBytes()
      lazyPreloadScheme(bytes, isOldSchemeNaming) { name, parser ->
        val attributeProvider = Function<String, String?> { parser.getAttributeValue(null, it) }
        val fileName = PathUtilRt.getFileName(url.path)
        val extension = getFileExtension(fileName, true)
        val externalInfo = ExternalInfo(fileName.substring(0, fileName.length - extension.length), extension)

        val schemeKey = name
                        ?: (processor as LazySchemeProcessor).getSchemeKey(attributeProvider, externalInfo.fileNameWithoutExtension)
                        ?: throw nameIsMissed(bytes)

        externalInfo.schemeKey = schemeKey

        val scheme = (processor as LazySchemeProcessor).createScheme(SchemeDataHolderImpl(processor, bytes, externalInfo), schemeKey, attributeProvider, true)
        val oldInfo = schemeToInfo.put(scheme, externalInfo)
        LOG.assertTrue(oldInfo == null)
        val oldScheme = schemeListManager.readOnlyExternalizableSchemes.put(schemeKey, scheme)
        if (oldScheme != null) {
          LOG.warn("Duplicated scheme $schemeKey - old: $oldScheme, new $scheme")
        }
        schemes.add(scheme)
        if (requestor is UITheme) {
          requestor.editorSchemeName = schemeKey
        }
        if (requestor is TempUIThemeBasedLookAndFeelInfo) {
          requestor.theme.editorSchemeName = schemeKey
        }
      }
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Throwable) {
      LOG.error("Cannot read scheme from $resourceName", e)
    }
  }

  internal fun createSchemeLoader(isDuringLoad: Boolean = false): SchemeLoader<T, MUTABLE_SCHEME> {
    val filesToDelete = ObjectOpenHashSet(filesToDelete)
    // caller must call SchemeLoader.apply to bring back scheduled for delete files
    this.filesToDelete.removeAll(filesToDelete)
    // SchemeLoader can use retain list to bring back previously  scheduled for delete file,
    // but what if someone will call save() during load and file will be deleted, although should be loaded by a new load session
    // (because modified on disk)
    return SchemeLoader(this, schemes, filesToDelete, isDuringLoad)
  }

  internal fun getFileExtension(fileName: CharSequence, isAllowAny: Boolean): String {
    return when {
      StringUtilRt.endsWithIgnoreCase(fileName, schemeExtension) -> schemeExtension
      StringUtilRt.endsWithIgnoreCase(fileName, FileStorageCoreUtil.DEFAULT_EXT) -> FileStorageCoreUtil.DEFAULT_EXT
      isAllowAny -> PathUtil.getFileExtension(fileName.toString())!!
      else -> throw IllegalStateException("Scheme file extension $fileName is unknown, must be filtered out")
    }
  }

  override fun loadSchemes(): Collection<T> {
    if (!isLoadingSchemes.compareAndSet(false, true)) {
      throw IllegalStateException("loadSchemes is already called")
    }

    try {
      // isDuringLoad is true even if loadSchemes called not first time, but on reload,
      // because scheme processor should use cumulative event `reloaded` to update runtime state/caches
      val schemeLoader = createSchemeLoader(isDuringLoad = true)
      val isLoadOnlyFromProvider = provider != null && provider.processChildren(fileSpec, roamingType, { canRead(it) }) { name, input, readOnly ->
        catchAndLog({ "${provider.javaClass.name}: $name" }) {
          val scheme = schemeLoader.loadScheme(name, input, null)
          if (readOnly && scheme != null) {
            schemeListManager.readOnlyExternalizableSchemes.put(processor.getSchemeKey(scheme), scheme)
          }
        }
        true
      }

      if (!isLoadOnlyFromProvider) {
        if (virtualFileResolver == null) {
          ioDirectory.directoryStreamIfExists({ canRead(it.fileName.toString()) }) { directoryStream ->
            for (file in directoryStream) {
              catchAndLog({ file.toString() }) {
                val bytes = try {
                  Files.readAllBytes(file)
                }
                catch (e: FileSystemException) {
                  when {
                    file.isDirectory() -> return@catchAndLog
                    else -> throw e
                  }
                }
                schemeLoader.loadScheme(file.fileName.toString(), null, bytes)
              }
            }
          }
        }
        else {
          for (file in getVirtualDirectory(StateStorageOperation.READ)?.children ?: VirtualFile.EMPTY_ARRAY) {
            catchAndLog({ file.path }) {
              if (canRead(file.nameSequence)) {
                schemeLoader.loadScheme(file.name, null, file.contentsToByteArray())
              }
            }
          }
        }
      }

      val newSchemes = schemeLoader.apply()
      for (newScheme in newSchemes) {
        if (processPendingCurrentSchemeName(newScheme)) {
          break
        }
      }

      fileChangeSubscriber?.invoke(this)

      return newSchemes
    }
    finally {
      isLoadingSchemes.set(false)
    }
  }

  override fun reload() {
    processor.beforeReloaded(this)
    // we must not remove non-persistent (e.g. predefined) schemes, because we cannot load it (obviously)
    // do not schedule scheme file removing because we just need to update our runtime state, not state on disk
    removeExternalizableSchemesFromRuntimeState()
    processor.reloaded(this, loadSchemes())
  }

  // method is used to reflect already performed changes on disk, so, `isScheduleToDelete = false` is passed to `retainExternalInfo`
  internal fun removeExternalizableSchemesFromRuntimeState() {
    // todo check is bundled/read-only schemes correctly handled
    val iterator = schemes.iterator()
    for (scheme in iterator) {
      if ((scheme as? SerializableScheme)?.schemeState ?: processor.getState(scheme) == SchemeState.NON_PERSISTENT) {
        continue
      }

      activeScheme?.let {
        if (scheme === it) {
          currentPendingSchemeName = processor.getSchemeKey(it)
          activeScheme = null
        }
      }

      iterator.remove()

      @Suppress("UNCHECKED_CAST")
      processor.onSchemeDeleted(scheme as MUTABLE_SCHEME)
    }
    retainExternalInfo(isScheduleToDelete = false)
  }

  internal fun getFileName(scheme: T) = schemeToInfo.get(scheme)?.fileNameWithoutExtension

  fun canRead(name: CharSequence) = (updateExtension && name.endsWith(FileStorageCoreUtil.DEFAULT_EXT, true) || name.endsWith(schemeExtension, ignoreCase = true)) && (processor !is LazySchemeProcessor || processor.isSchemeFile(name))

  override fun save(errors: MutableList<Throwable>) {
    if (isLoadingSchemes.get()) {
      LOG.warn("Skip save - schemes are loading")
    }

    var hasSchemes = false
    val nameGenerator = UniqueNameGenerator()
    val changedSchemes = SmartList<MUTABLE_SCHEME>()
    for (scheme in schemes) {
      val state = (scheme as? SerializableScheme)?.schemeState ?: processor.getState(scheme)
      if (state == SchemeState.NON_PERSISTENT) {
        continue
      }

      hasSchemes = true

      if (state != SchemeState.UNCHANGED) {
        @Suppress("UNCHECKED_CAST")
        changedSchemes.add(scheme as MUTABLE_SCHEME)
      }

      val fileName = getFileName(scheme)
      if (fileName != null && !isRenamed(scheme)) {
        nameGenerator.addExistingName(fileName)
      }
    }

    val filesToDelete = ObjectOpenHashSet(filesToDelete)
    for (scheme in changedSchemes) {
      try {
        saveScheme(scheme, nameGenerator, filesToDelete)
      }
      catch (e: Throwable) {
        errors.add(RuntimeException("Cannot save scheme $fileSpec/$scheme", e))
      }
    }

    if (!filesToDelete.isEmpty()) {
      val iterator = schemeToInfo.values.iterator()
      for (info in iterator) {
        if (filesToDelete.contains(info.fileName)) {
          iterator.remove()
        }
      }

      this.filesToDelete.removeAll(filesToDelete)
      deleteFiles(errors, filesToDelete)
      // remove empty directory only if some file was deleted - avoid check on each save
      if (!hasSchemes && (provider == null || !provider.isApplicable(fileSpec, roamingType))) {
        removeDirectoryIfEmpty(errors)
      }
    }
  }

  private fun removeDirectoryIfEmpty(errors: MutableList<Throwable>) {
    ioDirectory.directoryStreamIfExists {
      for (file in it) {
        if (!file.isHidden()) {
          LOG.info("Directory ${ioDirectory.fileName} is not deleted: at least one file ${file.fileName} exists")
          return@removeDirectoryIfEmpty
        }
      }
    }

    LOG.info("Remove scheme directory ${ioDirectory.fileName}")

    if (isUseVfs) {
      val dir = getVirtualDirectory(StateStorageOperation.WRITE)
      cachedVirtualDirectory = null
      if (dir != null) {
        runWriteAction {
          try {
            dir.delete(this)
          }
          catch (e: IOException) {
            errors.add(e)
          }
        }
      }
    }
    else {
      errors.catch {
        ioDirectory.delete()
      }
    }
  }

  private fun saveScheme(scheme: MUTABLE_SCHEME, nameGenerator: UniqueNameGenerator, filesToDelete: MutableSet<String>) {
    var externalInfo: ExternalInfo? = schemeToInfo.get(scheme)
    val currentFileNameWithoutExtension = externalInfo?.fileNameWithoutExtension
    val element = processor.writeScheme(scheme)?.let { it as? Element ?: (it as Document).detachRootElement() }
    if (element.isEmpty()) {
      externalInfo?.scheduleDelete(filesToDelete, "empty")
      return
    }

    var fileNameWithoutExtension = currentFileNameWithoutExtension
    if (fileNameWithoutExtension == null || isRenamed(scheme)) {
      fileNameWithoutExtension = nameGenerator.generateUniqueName(schemeNameToFileName(processor.getSchemeKey(scheme)))
    }

    val fileName = fileNameWithoutExtension + schemeExtension
    // file will be overwritten, so, we don't need to delete it
    filesToDelete.remove(fileName)

    val newDigest = element!!.digest()
    when {
      externalInfo != null && currentFileNameWithoutExtension === fileNameWithoutExtension && externalInfo.isDigestEquals(newDigest) -> return
      isEqualToBundledScheme(externalInfo, newDigest, scheme, filesToDelete) -> return

      // we must check it only here to avoid delete old scheme just because it is empty (old idea save -> new idea delete on open)
      processor is LazySchemeProcessor && processor.isSchemeDefault(scheme, newDigest) -> {
        externalInfo?.scheduleDelete(filesToDelete, "equals to default")
        return
      }
    }

    // stream provider always use LF separator
    val byteOut = element.toBufferExposingByteArray()

    var providerPath: String?
    if (provider != null && provider.enabled) {
      providerPath = "$fileSpec/$fileName"
      if (!provider.isApplicable(providerPath, roamingType)) {
        providerPath = null
      }
    }
    else {
      providerPath = null
    }

    // if another new scheme uses old name of this scheme, we must not delete it (as part of rename operation)
    @Suppress("SuspiciousEqualsCombination")
    val renamed = externalInfo != null && fileNameWithoutExtension !== currentFileNameWithoutExtension && currentFileNameWithoutExtension != null && nameGenerator.isUnique(currentFileNameWithoutExtension)
    if (providerPath == null) {
      if (isUseVfs) {
        var file: VirtualFile? = null
        var dir = getVirtualDirectory(StateStorageOperation.WRITE)
        if (dir == null || !dir.isValid) {
          dir = createDir(ioDirectory, this)
          cachedVirtualDirectory = dir
        }

        if (renamed) {
          val oldFile = dir.findChild(externalInfo!!.fileName)
          if (oldFile != null) {
            // VFS doesn't allow to rename to existing file, so, check it
            if (dir.findChild(fileName) == null) {
              runWriteAction {
                oldFile.rename(this, fileName)
              }
              file = oldFile
            }
            else {
              externalInfo.scheduleDelete(filesToDelete, "renamed")
            }
          }
        }

        if (file == null) {
          file = dir.getOrCreateChild(fileName, this)
        }

        runWriteAction {
          file.getOutputStream(this).use { byteOut.writeTo(it) }
        }
      }
      else {
        if (renamed) {
          externalInfo!!.scheduleDelete(filesToDelete, "renamed")
        }
        ioDirectory.resolve(fileName).write(byteOut.internalBuffer, 0, byteOut.size())
      }
    }
    else {
      if (renamed) {
        externalInfo!!.scheduleDelete(filesToDelete, "renamed")
      }
      provider!!.write(providerPath, byteOut.internalBuffer, byteOut.size(), roamingType)
    }

    if (externalInfo == null) {
      externalInfo = ExternalInfo(fileNameWithoutExtension, schemeExtension)
      schemeToInfo.put(scheme, externalInfo)
    }
    else {
      externalInfo.setFileNameWithoutExtension(fileNameWithoutExtension, schemeExtension)
    }
    externalInfo.digest = newDigest
    externalInfo.schemeKey = processor.getSchemeKey(scheme)
  }

  private fun isEqualToBundledScheme(externalInfo: ExternalInfo?, newDigest: ByteArray, scheme: MUTABLE_SCHEME, filesToDelete: MutableSet<String>): Boolean {
    fun serializeIfPossible(scheme: T): Element? {
      LOG.runAndLogException {
        @Suppress("UNCHECKED_CAST")
        val bundledAsMutable = scheme as? MUTABLE_SCHEME ?: return null
        return processor.writeScheme(bundledAsMutable) as Element
      }
      return null
    }

    val bundledScheme = schemeListManager.readOnlyExternalizableSchemes.get(processor.getSchemeKey(scheme))
    if (bundledScheme == null) {
      if ((processor as? LazySchemeProcessor)?.isSchemeEqualToBundled(scheme) == true) {
        externalInfo?.scheduleDelete(filesToDelete, "equals to bundled")
        return true
      }
      return false
    }

    val bundledExternalInfo = schemeToInfo.get(bundledScheme) ?: return false
    if (bundledExternalInfo.digest == null) {
      serializeIfPossible(bundledScheme)?.let {
        bundledExternalInfo.digest = it.digest()
      } ?: return false
    }
    if (bundledExternalInfo.isDigestEquals(newDigest)) {
      externalInfo?.scheduleDelete(filesToDelete, "equals to bundled")
      return true
    }
    return false
  }

  private fun isRenamed(scheme: T): Boolean {
    val info = schemeToInfo.get(scheme)
    return info != null && processor.getSchemeKey(scheme) != info.schemeKey
  }

  private fun deleteFiles(errors: MutableList<Throwable>, filesToDelete: MutableSet<String>) {
    if (provider != null) {
      val iterator = filesToDelete.iterator()
      for (name in iterator) {
        errors.catch {
          val spec = "$fileSpec/$name"
          if (provider.delete(spec, roamingType)) {
            LOG.debug { "$spec deleted from provider $provider" }
            iterator.remove()
          }
        }
      }
    }

    if (filesToDelete.isEmpty()) {
      return
    }

    LOG.debug { "Delete scheme files: ${filesToDelete.joinToString()}" }

    if (isUseVfs) {
      getVirtualDirectory(StateStorageOperation.WRITE)?.let { virtualDir ->
        val childrenToDelete = virtualDir.children.filter { filesToDelete.contains(it.name) }
        if (childrenToDelete.isNotEmpty()) {
          runWriteAction {
            for (file in childrenToDelete) {
              errors.catch { file.delete(this) }
            }
          }
        }
        return
      }
    }

    for (name in filesToDelete) {
      errors.catch { ioDirectory.resolve(name).delete() }
    }
  }

  internal fun getVirtualDirectory(reasonOperation: StateStorageOperation): VirtualFile? {
    var result = cachedVirtualDirectory
    if (result == null) {
      val path = ioDirectory.systemIndependentPath
      result = when (virtualFileResolver) {
        null -> LocalFileSystem.getInstance().findFileByPath(path)
        else -> virtualFileResolver.resolveVirtualFile(path, reasonOperation)
      }
      cachedVirtualDirectory = result
    }
    return result
  }

  override fun setSchemes(newSchemes: List<T>, newCurrentScheme: T?, removeCondition: Predicate<T>?) {
    schemeListManager.setSchemes(newSchemes, newCurrentScheme, removeCondition)
  }

  internal fun retainExternalInfo(isScheduleToDelete: Boolean) {
    if (schemeToInfo.isEmpty()) {
      return
    }

    val iterator = schemeToInfo.entries.iterator()
    l@ for ((scheme, info) in iterator) {
      if (schemeListManager.readOnlyExternalizableSchemes.get(processor.getSchemeKey(scheme)) === scheme) {
        continue
      }

      for (s in schemes) {
        if (s === scheme) {
          filesToDelete.remove(info.fileName)
          continue@l
        }
      }

      iterator.remove()
      if (isScheduleToDelete) {
        info.scheduleDelete(filesToDelete, "requested to delete")
      }
    }
  }

  override fun addScheme(scheme: T, replaceExisting: Boolean) = schemeListManager.addScheme(scheme, replaceExisting)

  override fun findSchemeByName(schemeName: String) = schemes.firstOrNull { processor.getSchemeKey(it) == schemeName }

  override fun removeScheme(name: String) = removeFirstScheme(true) { processor.getSchemeKey(it) == name }

  override fun removeScheme(scheme: T) = removeScheme(scheme, isScheduleToDelete = true)

  fun removeScheme(scheme: T, isScheduleToDelete: Boolean) = removeFirstScheme(isScheduleToDelete) { it === scheme } != null

  override fun isMetadataEditable(scheme: T) = !schemeListManager.readOnlyExternalizableSchemes.containsKey(processor.getSchemeKey(scheme))

  override fun toString() = fileSpec

  internal fun removeFirstScheme(isScheduleToDelete: Boolean, condition: (T) -> Boolean): T? {
    val iterator = schemes.iterator()
    for (scheme in iterator) {
      if (!condition(scheme)) {
        continue
      }

      if (activeScheme === scheme) {
        activeScheme = null
      }

      iterator.remove()

      if (isScheduleToDelete && processor.isExternalizable(scheme)) {
        schemeToInfo.remove(scheme)?.scheduleDelete(filesToDelete, "requested to delete (removeFirstScheme)")
      }
      return scheme
    }

    return null
  }
}