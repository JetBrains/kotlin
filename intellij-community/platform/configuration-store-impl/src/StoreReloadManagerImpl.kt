// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.configurationStore.schemeManager.SchemeChangeApplicator
import com.intellij.configurationStore.schemeManager.SchemeChangeEvent
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.StateStorage
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectReloadState
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.processOpenedProjects
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManagerListener
import com.intellij.ui.AppUIUtil
import com.intellij.util.ExceptionUtil
import com.intellij.util.SingleAlarm
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.LinkedHashSet

private val CHANGED_FILES_KEY = Key<LinkedHashMap<ComponentStoreImpl, LinkedHashSet<StateStorage>>>("CHANGED_FILES_KEY")
private val CHANGED_SCHEMES_KEY = Key<LinkedHashMap<SchemeChangeApplicator, LinkedHashSet<SchemeChangeEvent>>>("CHANGED_SCHEMES_KEY")

/**
 * This service is temporary allowed to be overridden to support reloading of new project model entities. It should be removed after merging
 * new project model modules to community project.
 */
@ApiStatus.Internal
open class StoreReloadManagerImpl : StoreReloadManager, Disposable {
  private val reloadBlockCount = AtomicInteger()
  private val blockStackTrace = AtomicReference<Throwable?>()
  private val changedApplicationFiles = LinkedHashSet<StateStorage>()

  private val changedFilesAlarm = SingleAlarm(Runnable {
    if (isReloadBlocked() || !tryToReloadApplication()) {
      return@Runnable
    }

    val projectsToReload = ObjectOpenHashSet<Project>()
    processOpenedProjects { project ->
      val changedSchemes = CHANGED_SCHEMES_KEY.getAndClear(project as UserDataHolderEx)
      val changedStorages = CHANGED_FILES_KEY.getAndClear(project as UserDataHolderEx)
      if ((changedSchemes == null || changedSchemes.isEmpty()) && (changedStorages == null || changedStorages.isEmpty())
          && !mayHaveAdditionalConfigurations(project)) {
        return@processOpenedProjects
      }

      runBatchUpdate(project.messageBus) {
        // reload schemes first because project file can refer to scheme (e.g. inspection profile)
        if (changedSchemes != null) {
          for ((tracker, files) in changedSchemes) {
            LOG.runAndLogException {
              tracker.reload(files)
            }
          }
        }

        if (changedStorages != null) {
          for ((store, storages) in changedStorages) {
            if ((store.storageManager as? StateStorageManagerImpl)?.componentManager?.isDisposed == true) {
              continue
            }

            @Suppress("UNCHECKED_CAST")
            if (reloadStore(storages, store) == ReloadComponentStoreStatus.RESTART_AGREED) {
              projectsToReload.add(project)
            }
          }
        }

        reloadAdditionalConfigurations(project)
      }
    }

    for (project in projectsToReload) {
      doReloadProject(project)
    }
  }, delay = 300, parentDisposable = this)

  protected open fun reloadAdditionalConfigurations(project: Project) {
  }

  protected open fun mayHaveAdditionalConfigurations(project: Project): Boolean = false

  internal class MyVirtualFileManagerListener : VirtualFileManagerListener {
    private val manager = StoreReloadManager.getInstance()

    override fun beforeRefreshStart(asynchronous: Boolean) {
      manager.blockReloadingProjectOnExternalChanges()
    }

    override fun afterRefreshFinish(asynchronous: Boolean) {
      manager.unblockReloadingProjectOnExternalChanges()
    }
  }

  override fun isReloadBlocked(): Boolean {
    val count = reloadBlockCount.get()
    LOG.debug { "[RELOAD] myReloadBlockCount = $count" }
    return count > 0
  }

  override fun saveChangedProjectFile(file: VirtualFile, project: Project) {
    val storageManager = (project.stateStore as ComponentStoreImpl).storageManager as? StateStorageManagerImpl ?: return
    storageManager.getCachedFileStorages(listOf(storageManager.collapseMacros(file.path))).firstOrNull()?.let {
      // if empty, so, storage is not yet loaded, so, we don't have to reload
      storageFilesChanged(mapOf(project to listOf(it)))
    }
  }

  override fun blockReloadingProjectOnExternalChanges() {
    if (reloadBlockCount.getAndIncrement() == 0 && !ApplicationInfoImpl.isInStressTest()) {
      blockStackTrace.set(Throwable())
    }
  }

  override fun unblockReloadingProjectOnExternalChanges() {
    val counter = reloadBlockCount.get()
    if (counter <= 0) {
      LOG.error("Block counter $counter must be > 0, first block stack trace: ${blockStackTrace.get()?.let { ExceptionUtil.getThrowableText(it) }}")
    }

    if (reloadBlockCount.decrementAndGet() != 0) {
      return
    }

    blockStackTrace.set(null)
    changedFilesAlarm.request()
  }

  /**
   * Internal use only. Force reload changed project files. Must be called before save otherwise saving maybe not performed (because storage saving is disabled).
   */
  override fun flushChangedProjectFileAlarm() {
    changedFilesAlarm.drainRequestsInTest()
  }

  override suspend fun reloadChangedStorageFiles() {
    val unfinishedRequest = changedFilesAlarm.getUnfinishedRequest() ?: return
    withContext(storeEdtCoroutineDispatcher) {
      unfinishedRequest.run()
      // just to be sure
      changedFilesAlarm.getUnfinishedRequest()?.run()
    }
  }

  override fun reloadProject(project: Project) {
    CHANGED_FILES_KEY.set(project, null)
    doReloadProject(project)
  }

  override fun storageFilesChanged(componentManagerToStorages: Map<ComponentManager, Collection<StateStorage>>) {
    if (componentManagerToStorages.isEmpty()) {
      return
    }

    if (LOG.isDebugEnabled) {
      LOG.debug("[RELOAD] registering to reload: ${componentManagerToStorages.map { "${it.key}: ${it.value.joinToString()}" }.joinToString("\n")}", Exception())
    }

    for ((componentManager, storages) in componentManagerToStorages) {
      val project: Project? = when (componentManager) {
        is Project -> componentManager
        is Module -> componentManager.project
        else -> null
      }

      if (project == null) {
        val changes = changedApplicationFiles
        synchronized(changes) {
          changes.addAll(storages)
        }
      }
      else {
        val changes = CHANGED_FILES_KEY.get(project) ?: (project as UserDataHolderEx).putUserDataIfAbsent(CHANGED_FILES_KEY, linkedMapOf())
        synchronized(changes) {
          changes.getOrPut(componentManager.stateStore as ComponentStoreImpl) { LinkedHashSet() }.addAll(storages)
        }
      }

      for (storage in storages) {
        if (storage is StateStorageBase<*>) {
          storage.disableSaving()
        }
      }
    }

    scheduleProcessingChangedFiles()
  }

  internal fun registerChangedSchemes(events: List<SchemeChangeEvent>, schemeFileTracker: SchemeChangeApplicator, project: Project) {
    if (LOG.isDebugEnabled) {
      LOG.debug("[RELOAD] Registering schemes to reload: $events", Exception())
    }

    val changes = CHANGED_SCHEMES_KEY.get(project) ?: (project as UserDataHolderEx).putUserDataIfAbsent(CHANGED_SCHEMES_KEY, linkedMapOf())
    synchronized(changes) {
      changes.getOrPut(schemeFileTracker) { LinkedHashSet() }.addAll(events)
    }

    scheduleProcessingChangedFiles()
  }

  override fun scheduleProcessingChangedFiles() {
    if (!isReloadBlocked()) {
      changedFilesAlarm.cancelAndRequest()
    }
  }

  private fun tryToReloadApplication(): Boolean {
    if (ApplicationManager.getApplication().isDisposed) {
      return false
    }

    if (changedApplicationFiles.isEmpty()) {
      return true
    }

    val changes = LinkedHashSet(changedApplicationFiles)
    changedApplicationFiles.clear()

    return reloadAppStore(changes)
  }

  override fun dispose() {
  }
}

fun reloadAppStore(changes: Set<StateStorage>): Boolean {
  val status = reloadStore(changes, ApplicationManager.getApplication().stateStore as ComponentStoreImpl)
  if (status == ReloadComponentStoreStatus.RESTART_AGREED) {
    ApplicationManagerEx.getApplicationEx().restart(true)
    return false
  }
  else {
    return status == ReloadComponentStoreStatus.SUCCESS || status == ReloadComponentStoreStatus.RESTART_CANCELLED
  }
}

internal fun reloadStore(changedStorages: Set<StateStorage>, store: ComponentStoreImpl): ReloadComponentStoreStatus {
  val notReloadableComponents: Collection<String>?
  var willBeReloaded = false
  try {
    try {
      notReloadableComponents = store.reload(changedStorages)
    }
    catch (e: Throwable) {
      LOG.warn(e)
      AppUIUtil.invokeOnEdt {
        Messages.showWarningDialog(ConfigurationStoreBundle.message("project.reload.failed", e.message), ConfigurationStoreBundle.message("project.reload.failed.title"))
      }
      return ReloadComponentStoreStatus.ERROR
    }

    if (notReloadableComponents == null || notReloadableComponents.isEmpty()) {
      return ReloadComponentStoreStatus.SUCCESS
    }

    willBeReloaded = askToRestart(store, notReloadableComponents, changedStorages, store.project == null)
    return if (willBeReloaded) ReloadComponentStoreStatus.RESTART_AGREED else ReloadComponentStoreStatus.RESTART_CANCELLED
  }
  finally {
    if (!willBeReloaded) {
      for (storage in changedStorages) {
        if (storage is StateStorageBase<*>) {
          storage.enableSaving()
        }
      }
    }
  }
}

// used in settings repository plugin
fun askToRestart(store: IComponentStore, notReloadableComponents: Collection<String>, changedStorages: Set<StateStorage>?, isApp: Boolean): Boolean {
  val message = StringBuilder()
  val storeName = if (store is IProjectStore) "Project '${store.projectName}'" else "Application"
  message.append(storeName).append(' ')
  message.append("components were changed externally and cannot be reloaded:\n\n")
  var count = 0
  for (component in notReloadableComponents) {
    if (count == 10) {
      message.append('\n').append("and ").append(notReloadableComponents.size - count).append(" more").append('\n')
    }
    else {
      message.append(component).append('\n')
      count++
    }
  }

  message.append("\nWould you like to ")
  if (isApp) {
    message.append(if (ApplicationManager.getApplication().isRestartCapable) "restart" else "shutdown").append(' ')
    message.append(ApplicationNamesInfo.getInstance().productName).append('?')
  }
  else {
    message.append("reload project?")
  }

  if (Messages.showYesNoDialog(message.toString(), "$storeName Files Changed", Messages.getQuestionIcon()) != Messages.YES) {
    return false
  }

  if (changedStorages != null) {
    for (storage in changedStorages) {
      if (storage is StateStorageBase<*>) {
        storage.disableSaving()
      }
    }
  }
  return true
}

internal enum class ReloadComponentStoreStatus {
  RESTART_AGREED,
  RESTART_CANCELLED,
  ERROR,
  SUCCESS
}

private fun <T : Any> Key<T>.getAndClear(holder: UserDataHolderEx): T? {
  val value = holder.getUserData(this) ?: return null
  holder.replace(this, value, null)
  return value
}

private fun doReloadProject(project: Project) {
  val projectRef = Ref.create(project)
  ProjectReloadState.getInstance(project).onBeforeAutomaticProjectReload()
  AppUIExecutor.onWriteThread(ModalityState.NON_MODAL).later().submit {
    LOG.debug("Reloading project.")
    val project1 = projectRef.get()
    // Let it go
    projectRef.set(null)

    if (project1.isDisposed) {
      return@submit
    }

    // must compute here, before project dispose
    val presentableUrl = project1.presentableUrl!!
    if (!ProjectManagerEx.getInstanceEx().closeAndDispose(project1)) {
      return@submit
    }

    ProjectManagerEx.getInstanceEx().openProject(Paths.get(presentableUrl), OpenProjectTask())
  }
}