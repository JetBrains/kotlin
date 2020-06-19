// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths

private const val FILE_SPEC = "${APP_CONFIG}/project.default.xml"

private class DefaultProjectStorage(file: Path, fileSpec: String, pathMacroManager: PathMacroManager) : FileBasedStorage(file, fileSpec, "defaultProject", pathMacroManager.createTrackingSubstitutor(), RoamingType.DISABLED) {
  override val configuration = object: FileBasedStorageConfiguration {
    override val isUseVfsForRead: Boolean
      get() = false

    override val isUseVfsForWrite: Boolean
      get() = false
  }

  public override fun loadLocalData(): Element? {
    val element = super.loadLocalData() ?: return null
    try {
      return element.getChild("component")?.getChild("defaultProject")
    }
    catch (e: NullPointerException) {
      LOG.warn("Cannot read default project")
      return null
    }
  }

  override fun createSaveSession(states: StateMap) = object : FileBasedStorage.FileSaveSession(states, this) {
    override fun saveLocally(dataWriter: DataWriter?) {
      super.saveLocally(when (dataWriter) {
        null -> null
        else -> object : StringDataWriter() {
          override fun hasData(filter: DataWriterFilter) = dataWriter.hasData(filter)

          override fun write(writer: Writer, lineSeparator: String, filter: DataWriterFilter?) {
            val lineSeparatorWithIndent = "$lineSeparator    "
            writer.append("<application>").append(lineSeparator)
            writer.append("""  <component name="ProjectManager">""")
            writer.append(lineSeparatorWithIndent)
            (dataWriter as StringDataWriter).write(writer, lineSeparatorWithIndent, filter)
            writer.append(lineSeparator)
            writer.append("  </component>").append(lineSeparator)
            writer.append("</application>")
          }
        }
      })
    }
  }
}

// cannot be `internal`, used in Upsource
@ApiStatus.Internal
class DefaultProjectStoreImpl(override val project: Project) : ChildlessComponentStore() {
  // see note about default state in project store
  override val loadPolicy: StateLoadPolicy
    get() = if (ApplicationManager.getApplication().isUnitTestMode) StateLoadPolicy.NOT_LOAD else StateLoadPolicy.LOAD

  private val storage by lazy {
    DefaultProjectStorage(Paths.get(ApplicationManager.getApplication().stateStore.storageManager.expandMacros(FILE_SPEC)), FILE_SPEC, PathMacroManager.getInstance(project))
  }

  override val storageManager = object : StateStorageManager {
    override val componentManager: ComponentManager?
      get() = null

    override fun addStreamProvider(provider: StreamProvider, first: Boolean) {
    }

    override fun removeStreamProvider(clazz: Class<out StreamProvider>) {
    }

    override fun rename(path: String, newName: String) {
    }

    override fun getStateStorage(storageSpec: Storage) = storage

    override fun expandMacros(path: String) = throw UnsupportedOperationException()

    override fun getOldStorage(component: Any, componentName: String, operation: StateStorageOperation) = storage
  }

  override fun isUseLoadedStateAsExisting(storage: StateStorage) = false

  // don't want to optimize and use already loaded data - it will add unnecessary complexity and implementation-lock (currently we store loaded archived state in memory, but later implementation can be changed)
  fun getStateCopy() = storage.loadLocalData()

  override fun getPathMacroManagerForDefaults() = PathMacroManager.getInstance(project)

  override fun <T> getStorageSpecs(component: PersistentStateComponent<T>, stateSpec: State, operation: StateStorageOperation) = listOf(PROJECT_FILE_STORAGE_ANNOTATION)

  override fun setPath(path: Path) {
  }

  override fun toString() = "default project"
}

// ExportSettingsAction checks only "State" annotation presence, but doesn't require PersistentStateComponent implementation, so, we can just specify annotation
@State(name = "ProjectManager", storages = [(Storage(FILE_SPEC))])
internal class DefaultProjectExportableAndSaveTrigger {
  suspend fun save(forceSavingAllSettings: Boolean): SaveResult {
    val result = SaveResult()
    (ProjectManagerEx.getInstanceEx().defaultProject.stateStore as ComponentStoreImpl).doSave(result, forceSavingAllSettings)
    return result
  }
}