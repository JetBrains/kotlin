// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEvents
import com.intellij.diagnostic.PluginException
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.DecodeDefaultsUtil
import com.intellij.openapi.components.*
import com.intellij.openapi.components.StateStorageChooserEx.Resolution
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.impl.stores.UnknownMacroNotification
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.ui.AppUIUtil
import com.intellij.util.ArrayUtilRt
import com.intellij.util.SmartList
import com.intellij.util.SystemProperties
import com.intellij.util.ThreeState
import com.intellij.util.containers.SmartHashSet
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.messages.MessageBus
import com.intellij.util.xmlb.XmlSerializerUtil
import gnu.trove.THashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import com.intellij.openapi.util.Pair as JBPair

internal val LOG = logger<ComponentStoreImpl>()
private val SAVE_MOD_LOG = Logger.getInstance("#configurationStore.save.skip")

internal val deprecatedComparator = Comparator<Storage> { o1, o2 ->
  val w1 = if (o1.deprecated) 1 else 0
  val w2 = if (o2.deprecated) 1 else 0
  w1 - w2
}

private class PersistenceStateAdapter(val component: Any) : PersistentStateComponent<Any> {
  override fun getState() = component

  override fun loadState(state: Any) {
    XmlSerializerUtil.copyBean(state, component)
  }
}

private val NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD_DEFAULT = TimeUnit.MINUTES.toSeconds(4).toInt()
private var NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD = NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD_DEFAULT

@TestOnly
internal fun restoreDefaultNotRoamableComponentSaveThreshold() {
  NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD = NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD_DEFAULT
}

@TestOnly
internal fun setRoamableComponentSaveThreshold(thresholdInSeconds: Int) {
  NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD = thresholdInSeconds
}

@ApiStatus.Internal
abstract class ComponentStoreImpl : IComponentStore {
  private val components = Collections.synchronizedMap(THashMap<String, ComponentInfo>())

  open val project: Project?
    get() = null

  open val loadPolicy: StateLoadPolicy
    get() = StateLoadPolicy.LOAD

  abstract override val storageManager: StateStorageManager

  internal fun getComponents(): Map<String, ComponentInfo> = components

  override fun initComponent(component: Any, serviceDescriptor: ServiceDescriptor?) {
    var componentName = ""
    try {
      @Suppress("DEPRECATION")
      if (component is PersistentStateComponent<*>) {
        componentName = initPersistenceStateComponent(component, getStateSpec(component), serviceDescriptor)
        component.initializeComponent()
      }
      else if (component is com.intellij.openapi.util.JDOMExternalizable) {
        componentName = ComponentManagerImpl.getComponentName(component)
        initJdomExternalizable(component, componentName)
      }
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Exception) {
      PluginException.logPluginError(LOG, "Cannot init $componentName component state", e, component.javaClass)
    }
  }

  override fun initPersistencePlainComponent(component: Any, key: String) {
    initPersistenceStateComponent(PersistenceStateAdapter(component),
                                  StateAnnotation(key, FileStorageAnnotation(StoragePathMacros.WORKSPACE_FILE, false)),
                                  serviceDescriptor = null)
  }

  private fun initPersistenceStateComponent(component: PersistentStateComponent<*>, stateSpec: State, serviceDescriptor: ServiceDescriptor?): String {
    val componentName = stateSpec.name
    val info = doAddComponent(componentName, component, stateSpec, serviceDescriptor)
    if (initComponent(info, null, ThreeState.NO) && serviceDescriptor != null) {
      // if not service, so, component manager will check it later for all components
      project?.let {
        val app = ApplicationManager.getApplication()
        if (!app.isHeadlessEnvironment && !app.isUnitTestMode && it.isInitialized) {
          notifyUnknownMacros(this, it, componentName)
        }
      }
    }
    return componentName
  }

  override suspend fun save(forceSavingAllSettings: Boolean) {
    val result = SaveResult()
    doSave(result, forceSavingAllSettings)
    result.throwIfErrored()
  }

  internal abstract suspend fun doSave(result: SaveResult, forceSavingAllSettings: Boolean)

  internal suspend inline fun <T> withEdtContext(crossinline task: suspend () -> T): T {
    return withEdtContext(storageManager.componentManager, task)
  }

  internal suspend fun createSaveSessionManagerAndSaveComponents(saveResult: SaveResult, forceSavingAllSettings: Boolean): SaveSessionProducerManager {
    return withEdtContext {
      val errors = SmartList<Throwable>()
      val manager = doCreateSaveSessionManagerAndCommitComponents(forceSavingAllSettings, errors)
      saveResult.addErrors(errors)
      manager
    }
  }

  @CalledInAwt
  internal fun doCreateSaveSessionManagerAndCommitComponents(isForce: Boolean, errors: MutableList<Throwable>): SaveSessionProducerManager {
    val saveManager = createSaveSessionProducerManager()
    commitComponents(isForce, saveManager, errors)
    return saveManager
  }

  @CalledInAwt
  internal open fun commitComponents(isForce: Boolean, session: SaveSessionProducerManager, errors: MutableList<Throwable>) {
    if (components.isEmpty()) {
      return
    }

    val isUseModificationCount = Registry.`is`("store.save.use.modificationCount", true)

    val names = ArrayUtilRt.toStringArray(components.keys)
    Arrays.sort(names)
    var timeLog: StringBuilder? = null

    // well, strictly speaking each component saving takes some time, but +/- several seconds doesn't matter
    val nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toInt()
    val isSaveModLogEnabled = SAVE_MOD_LOG.isDebugEnabled && !ApplicationManager.getApplication().isUnitTestMode
    for (name in names) {
      val start = System.currentTimeMillis()
      try {
        val info = components.get(name)!!
        var currentModificationCount = -1L

        if (info.isModificationTrackingSupported) {
          currentModificationCount = info.currentModificationCount
          if (currentModificationCount == info.lastModificationCount) {
            SAVE_MOD_LOG.debug { "${if (isUseModificationCount) "Skip " else ""}$name: modificationCount ${currentModificationCount} equals to last saved" }
            if (isUseModificationCount) {
              continue
            }
          }
        }

        if (info.lastSaved != -1) {
          if (isForce || (nowInSeconds - info.lastSaved) > NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD) {
            info.lastSaved = nowInSeconds
          }
          else {
            if (isSaveModLogEnabled) {
              SAVE_MOD_LOG.debug("Skip $name: was already saved in last ${TimeUnit.SECONDS.toMinutes(NOT_ROAMABLE_COMPONENT_SAVE_THRESHOLD_DEFAULT.toLong())} minutes" +
                                 " (lastSaved ${info.lastSaved}, now: $nowInSeconds)")
            }
            continue
          }
        }

        commitComponent(session, info, name)
        info.updateModificationCount(currentModificationCount)
      }
      catch (e: Throwable) {
        errors.add(Exception("Cannot get $name component state", e))
      }

      val duration = System.currentTimeMillis() - start
      if (duration > 10) {
        if (timeLog == null) {
          timeLog = StringBuilder("Saving " + toString())
        }
        else {
          timeLog.append(", ")
        }
        timeLog.append(name).append(" took ").append(duration).append(" ms")
      }
    }

    if (timeLog != null) {
      LOG.info(timeLog.toString())
    }
  }

  @TestOnly
  @CalledInAwt
  override fun saveComponent(component: PersistentStateComponent<*>) {
    val stateSpec = getStateSpec(component)
    LOG.debug { "saveComponent is called for ${stateSpec.name}" }
    val saveManager = createSaveSessionProducerManager()
    commitComponent(saveManager, ComponentInfoImpl(component, stateSpec), null)
    val absolutePath = Paths.get(storageManager.expandMacros(findNonDeprecated(stateSpec.storages).path)).toAbsolutePath().toString()
    val newDisposable = Disposer.newDisposable()
    try {
      VfsRootAccess.allowRootAccess(newDisposable, absolutePath)
      runBlocking {
        val saveResult = saveManager.save()
        saveResult.throwIfErrored()

        if (!saveResult.isChanged) {
          LOG.info("saveApplicationComponent is called for ${stateSpec.name} but nothing to save")
        }
      }
    }
    finally {
      Disposer.dispose(newDisposable)
    }
  }

  internal open fun createSaveSessionProducerManager() = SaveSessionProducerManager()

  private fun commitComponent(session: SaveSessionProducerManager, info: ComponentInfo, componentName: String?) {
    val component = info.component
    @Suppress("DEPRECATION")
    if (component is com.intellij.openapi.util.JDOMExternalizable) {
      val effectiveComponentName = componentName ?: ComponentManagerImpl.getComponentName(component)
      storageManager.getOldStorage(component, effectiveComponentName, StateStorageOperation.WRITE)?.let {
        session.getProducer(it)?.setState(component, effectiveComponentName, component)
      }
      return
    }

    var state: Any? = null
    // state can be null, so, we cannot compare to null to check is state was requested or not
    var stateRequested = false

    val stateSpec = info.stateSpec!!
    val effectiveComponentName = componentName ?: stateSpec.name
    val stateStorageChooser = component as? StateStorageChooserEx
    @Suppress("UNCHECKED_CAST")
    val storageSpecs = getStorageSpecs(component as PersistentStateComponent<Any>, stateSpec, StateStorageOperation.WRITE)
    for (storageSpec in storageSpecs) {
      @Suppress("IfThenToElvis")
      var resolution = if (stateStorageChooser == null) Resolution.DO else stateStorageChooser.getResolution(storageSpec, StateStorageOperation.WRITE)
      if (resolution == Resolution.SKIP) {
        continue
      }

      val storage = storageManager.getStateStorage(storageSpec)

      if (resolution == Resolution.DO) {
        resolution = storage.getResolution(component, StateStorageOperation.WRITE)
        if (resolution == Resolution.SKIP) {
          continue
        }
      }

      val sessionProducer = session.getProducer(storage) ?: continue
      if (storageSpec.deprecated || resolution == Resolution.CLEAR) {
        sessionProducer.setState(component, effectiveComponentName, null)
      }
      else {
        if (!stateRequested) {
          stateRequested = true
          state = (info.component as PersistentStateComponent<*>).state
        }

        setStateToSaveSessionProducer(state, info, effectiveComponentName, sessionProducer)
      }
    }
  }

  // method is not called if storage is deprecated or clear was requested (state in these cases is null), but called if state is null if returned so from component
  protected open fun setStateToSaveSessionProducer(state: Any?, info: ComponentInfo, effectiveComponentName: String, sessionProducer: SaveSessionProducer) {
    sessionProducer.setState(info.component, effectiveComponentName, state)
  }

  private fun initJdomExternalizable(@Suppress("DEPRECATION") component: com.intellij.openapi.util.JDOMExternalizable, componentName: String): String? {
    doAddComponent(componentName, component, stateSpec = null, serviceDescriptor = null)

    if (loadPolicy != StateLoadPolicy.LOAD) {
      return null
    }

    try {
      getDefaultState(component, componentName, Element::class.java)?.let { component.readExternal(it) }
    }
    catch (e: Throwable) {
      LOG.error(e)
    }

    val element = storageManager
                    .getOldStorage(component, componentName, StateStorageOperation.READ)
                    ?.getState(component, componentName, Element::class.java, null, false)
                  ?: return null
    try {
      component.readExternal(element)
    }
    catch (e: InvalidDataException) {
      LOG.error(e)
      return null
    }
    return componentName
  }

  private fun doAddComponent(name: String, component: Any, stateSpec: State?, serviceDescriptor: ServiceDescriptor?): ComponentInfo {
    val newInfo = createComponentInfo(component, stateSpec, serviceDescriptor)
    val existing = components.put(name, newInfo)
    if (existing != null && existing.component !== component) {
      components.put(name, existing)
      LOG.error("Conflicting component name '$name': ${existing.component.javaClass} and ${component.javaClass}")
      return existing
    }
    return newInfo
  }

  private fun initComponent(info: ComponentInfo, changedStorages: Set<StateStorage>?, reloadData: ThreeState): Boolean {
    return when {
      loadPolicy == StateLoadPolicy.NOT_LOAD -> false
      doInitComponent(info, changedStorages, reloadData) -> {
        // if component was initialized, update lastModificationCount
        info.updateModificationCount()
        true
      }
      else -> false
    }
  }

  private fun doInitComponent(info: ComponentInfo, changedStorages: Set<StateStorage>?, reloadData: ThreeState): Boolean {
    val stateSpec = info.stateSpec!!
    @Suppress("UNCHECKED_CAST")
    val component = info.component as PersistentStateComponent<Any>
    val name = stateSpec.name
    @Suppress("UNCHECKED_CAST")
    val stateClass: Class<Any> = when (component) {
      is PersistenceStateAdapter -> component.component::class.java as Class<Any>
      else -> ComponentSerializationUtil.getStateClass<Any>(component.javaClass)
    }
    if (!stateSpec.defaultStateAsResource && LOG.isDebugEnabled && getDefaultState(component, name, stateClass) != null) {
      LOG.error("$name has default state, but not marked to load it")
    }

    val defaultState = if (stateSpec.defaultStateAsResource) getDefaultState(component, name, stateClass) else null
    if (loadPolicy == StateLoadPolicy.LOAD) {
      val storageChooser = component as? StateStorageChooserEx
      for (storageSpec in getStorageSpecs(component, stateSpec, StateStorageOperation.READ)) {
        if (storageChooser?.getResolution(storageSpec, StateStorageOperation.READ) == Resolution.SKIP) {
          continue
        }

        val storage = storageManager.getStateStorage(storageSpec)

        // if storage marked as changed, it means that analyzeExternalChangesAndUpdateIfNeeded was called for it and storage is already reloaded
        val isReloadDataForStorage = if (reloadData == ThreeState.UNSURE) changedStorages!!.contains(storage) else reloadData.toBoolean()

        val stateGetter = doCreateStateGetter(isReloadDataForStorage, storage, info, name, stateClass)
        var state = stateGetter.getState(defaultState)
        if (state == null) {
          if (changedStorages != null && changedStorages.contains(storage)) {
            // state will be null if file deleted
            // we must create empty (initial) state to reinit component
            state = deserializeState(Element("state"), stateClass, null)!!
          }
          else {
            FeatureUsageSettingsEvents.logDefaultConfigurationState(name, stateSpec, stateClass, project)
            continue
          }
        }

        component.loadState(state)
        val stateAfterLoad = stateGetter.archiveState()
        LOG.runAndLogException {
          FeatureUsageSettingsEvents.logConfigurationState(name, stateSpec, stateAfterLoad ?: state, project)
        }
        return true
      }
    }

    // we load default state even if isLoadComponentState false - required for app components (for example, at least one color scheme must exists)
    if (defaultState == null) {
      component.noStateLoaded()
    }
    else {
      component.loadState(defaultState)
    }
    return true
  }

  protected open fun doCreateStateGetter(reloadData: Boolean,
                                         storage: StateStorage,
                                         info: ComponentInfo,
                                         name: String,
                                         stateClass: Class<Any>): StateGetter<Any> {
    // use.loaded.state.as.existing used in upsource
    val isUseLoadedStateAsExisting = info.stateSpec!!.useLoadedStateAsExisting && isUseLoadedStateAsExisting(storage)
    @Suppress("UNCHECKED_CAST")
    return createStateGetter(isUseLoadedStateAsExisting, storage, info.component as PersistentStateComponent<Any>, name, stateClass, reloadData)
  }

  protected open fun isUseLoadedStateAsExisting(storage: StateStorage): Boolean {
    return (storage as? XmlElementStorage)?.roamingType != RoamingType.DISABLED
           && SystemProperties.getBooleanProperty("use.loaded.state.as.existing", true)
  }

  protected open fun getPathMacroManagerForDefaults(): PathMacroManager? = null

  private fun <T : Any> getDefaultState(component: Any, componentName: String, stateClass: Class<T>): T? {
    val url = DecodeDefaultsUtil.getDefaults(component, componentName) ?: return null
    try {
      val element = JDOMUtil.load(url)
      getPathMacroManagerForDefaults()?.expandPaths(element)
      return deserializeState(element, stateClass, null)
    }
    catch (e: Throwable) {
      throw IOException("Error loading default state from $url", e)
    }
  }

  protected open fun <T> getStorageSpecs(component: PersistentStateComponent<T>,
                                         stateSpec: State,
                                         operation: StateStorageOperation): List<Storage> {
    val storages = stateSpec.storages
    if (storages.size == 1 || component is StateStorageChooserEx) {
      return storages.toList()
    }

    if (storages.isEmpty()) {
      if (stateSpec.defaultStateAsResource) {
        return emptyList()
      }

      throw AssertionError("No storage specified")
    }
    return storages.sortByDeprecated()
  }

  final override fun isReloadPossible(componentNames: Set<String>): Boolean = !componentNames.any { isNotReloadable(it) }

  private fun isNotReloadable(name: String): Boolean {
    val component = components.get(name)?.component ?: return false
    return component !is PersistentStateComponent<*> || !getStateSpec(component).reloadable
  }

  fun getNotReloadableComponents(componentNames: Collection<String>): Collection<String> {
    var notReloadableComponents: MutableSet<String>? = null
    for (componentName in componentNames) {
      if (isNotReloadable(componentName)) {
        if (notReloadableComponents == null) {
          notReloadableComponents = LinkedHashSet()
        }
        notReloadableComponents.add(componentName)
      }
    }
    return notReloadableComponents ?: emptySet()
  }

  final override fun reloadStates(componentNames: Set<String>, messageBus: MessageBus) {
    runBatchUpdate(messageBus) {
      reinitComponents(componentNames)
    }
  }

  final override fun reloadState(componentClass: Class<out PersistentStateComponent<*>>) {
    val stateSpec = getStateSpecOrError(componentClass)
    val info = components.get(stateSpec.name) ?: return
    (info.component as? PersistentStateComponent<*>)?.let {
      initComponent(info, emptySet(), ThreeState.YES)
    }
  }

  private fun reloadState(componentName: String, changedStorages: Set<StateStorage>): Boolean {
    val info = components.get(componentName) ?: return false
    if (info.component !is PersistentStateComponent<*>) {
      return false
    }

    val isChangedStoragesEmpty = changedStorages.isEmpty()
    initComponent(info, if (isChangedStoragesEmpty) null else changedStorages, ThreeState.UNSURE)
    return true
  }

  /**
   * null if reloaded
   * empty list if nothing to reload
   * list of not reloadable components (reload is not performed)
   */
  fun reload(changedStorages: Set<StateStorage>): Collection<String>? {
    if (changedStorages.isEmpty()) {
      return emptySet()
    }

    val componentNames = SmartHashSet<String>()
    for (storage in changedStorages) {
      LOG.runAndLogException {
        // we must update (reload in-memory storage data) even if non-reloadable component will be detected later
        // not saved -> user does own modification -> new (on disk) state will be overwritten and not applied
        storage.analyzeExternalChangesAndUpdateIfNeeded(componentNames)
      }
    }

    if (componentNames.isEmpty) {
      return emptySet()
    }

    val notReloadableComponents = getNotReloadableComponents(componentNames)
    reinitComponents(componentNames, changedStorages, notReloadableComponents)
    return if (notReloadableComponents.isEmpty()) null else notReloadableComponents
  }

  // used in settings repository plugin
  /**
   * You must call it in batch mode (use runBatchUpdate)
   */
  fun reinitComponents(componentNames: Set<String>,
                       changedStorages: Set<StateStorage> = emptySet(),
                       notReloadableComponents: Collection<String> = emptySet()) {
    for (componentName in componentNames) {
      if (!notReloadableComponents.contains(componentName)) {
        reloadState(componentName, changedStorages)
      }
    }
  }

  @TestOnly
  fun removeComponent(name: String) {
    components.remove(name)
  }

  override fun toString() = storageManager.componentManager.toString()
}

private fun findNonDeprecated(storages: Array<Storage>) = storages.firstOrNull { !it.deprecated } ?: throw AssertionError(
  "All storages are deprecated")

enum class StateLoadPolicy {
  LOAD, LOAD_ONLY_DEFAULT, NOT_LOAD
}

internal fun Array<out Storage>.sortByDeprecated(): List<Storage> {
  if (size < 2) {
    return toList()
  }

  if (!first().deprecated) {
    val othersAreDeprecated = (1 until size).any { get(it).deprecated }
    if (othersAreDeprecated) {
      return toList()
    }
  }

  return sortedWith(deprecatedComparator)
}

private fun notifyUnknownMacros(store: IComponentStore, project: Project, componentName: String) {
  val substitutor = store.storageManager.macroSubstitutor as? TrackingPathMacroSubstitutor ?: return

  val immutableMacros = substitutor.getUnknownMacros(componentName)
  if (immutableMacros.isEmpty()) {
    return
  }

  val macros = LinkedHashSet(immutableMacros)
  AppUIUtil.invokeOnEdt(Runnable {
    var notified: MutableList<String>? = null
    val manager = NotificationsManager.getNotificationsManager()
    for (notification in manager.getNotificationsOfType(UnknownMacroNotification::class.java, project)) {
      if (notified == null) {
        notified = SmartList<String>()
      }
      notified.addAll(notification.macros)
    }
    if (!notified.isNullOrEmpty()) {
      macros.removeAll(notified!!)
    }

    if (macros.isEmpty()) {
      return@Runnable
    }

    LOG.debug("Reporting unknown path macros $macros in component $componentName")
    doNotify(macros, project, Collections.singletonMap(substitutor, store))
  }, project.disposed)
}

// to make sure that ApplicationStore or ProjectStore will not call incomplete doSave implementation
// (because these stores combine several calls for better control/async instead of simple sequential delegation)
abstract class ChildlessComponentStore : ComponentStoreImpl() {
  override suspend fun doSave(result: SaveResult, forceSavingAllSettings: Boolean) {
    childlessSaveImplementation(result, forceSavingAllSettings)
  }
}

internal suspend fun ComponentStoreImpl.childlessSaveImplementation(result: SaveResult, forceSavingAllSettings: Boolean) {
  createSaveSessionManagerAndSaveComponents(result, forceSavingAllSettings)
    .save()
    .appendTo(result)
}

internal suspend inline fun <T> withEdtContext(disposable: ComponentManager?, crossinline task: suspend () -> T): T {
  return withContext(storeEdtCoroutineContext) {
    @Suppress("NullableBooleanElvis")
    if (disposable?.isDisposed ?: false) {
      throw CancellationException()
    }

    task()
  }
}