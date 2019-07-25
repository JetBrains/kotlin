// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.*
import com.intellij.ide.startup.StartupManagerEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.options.Scheme
import com.intellij.openapi.options.SchemeManager
import com.intellij.openapi.options.SchemeManagerFactory
import com.intellij.openapi.options.SchemeProcessor
import com.intellij.openapi.project.DumbAwareRunnable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.lang.CompoundRuntimeException
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import java.nio.file.Paths

const val ROOT_CONFIG = "\$ROOT_CONFIG$"

internal typealias FileChangeSubscriber = (schemeManager: SchemeManagerImpl<*, *>) -> Unit

sealed class SchemeManagerFactoryBase : SchemeManagerFactory(), com.intellij.openapi.components.SettingsSavingComponent {
  private val managers = ContainerUtil.createLockFreeCopyOnWriteList<SchemeManagerImpl<Scheme, Scheme>>()

  protected open val componentManager: ComponentManager? = null

  protected open fun createFileChangeSubscriber(): FileChangeSubscriber? = null

  protected open fun getVirtualFileResolver(): VirtualFileResolver? = null

  final override fun <T : Any, MutableT : T> create(directoryName: String,
                                                    processor: SchemeProcessor<T, MutableT>,
                                                    presentableName: String?,
                                                    roamingType: RoamingType,
                                                    schemeNameToFileName: SchemeNameToFileName,
                                                    streamProvider: StreamProvider?,
                                                    directoryPath: Path?,
                                                    isAutoSave: Boolean): SchemeManager<T> {
    val path = checkPath(directoryName)
    val fileChangeSubscriber = when {
      streamProvider != null && streamProvider.isApplicable(path, roamingType) -> null
      else -> createFileChangeSubscriber()
    }
    val manager = SchemeManagerImpl(path,
                                    processor,
                                    streamProvider ?: (componentManager?.stateStore?.storageManager as? StateStorageManagerImpl)?.compoundStreamProvider,
                                    ioDirectory = directoryPath ?: pathToFile(path),
                                    roamingType = roamingType,
                                    presentableName = presentableName,
                                    schemeNameToFileName = schemeNameToFileName,
                                    fileChangeSubscriber = fileChangeSubscriber,
                                    virtualFileResolver = getVirtualFileResolver())
    if (isAutoSave) {
      @Suppress("UNCHECKED_CAST")
      managers.add(manager as SchemeManagerImpl<Scheme, Scheme>)
    }
    return manager
  }

  override fun dispose(schemeManager: SchemeManager<*>) {
    managers.remove(schemeManager)
  }

  open fun checkPath(originalPath: String): String {
    when {
      originalPath.contains('\\') -> LOG.error("Path must be system-independent, use forward slash instead of backslash")
      originalPath.isEmpty() -> LOG.error("Path must not be empty")
    }
    return originalPath
  }

  abstract fun pathToFile(path: String): Path

  fun process(processor: (SchemeManagerImpl<Scheme, Scheme>) -> Unit) {
    for (manager in managers) {
      try {
        processor(manager)
      }
      catch (e: Throwable) {
        LOG.error("Cannot reload settings for ${manager.javaClass.name}", e)
      }
    }
  }

  final override fun save() {
    val errors = SmartList<Throwable>()
    for (registeredManager in managers) {
      try {
        registeredManager.save(errors)
      }
      catch (e: Throwable) {
        errors.add(e)
      }
    }
    CompoundRuntimeException.throwIfNotEmpty(errors)
  }

  @Suppress("unused")
  private class ApplicationSchemeManagerFactory : SchemeManagerFactoryBase() {
    override val componentManager: ComponentManager
      get() = ApplicationManager.getApplication()

    override fun checkPath(originalPath: String): String {
      var path = super.checkPath(originalPath)
      if (path.startsWith(ROOT_CONFIG)) {
        path = path.substring(ROOT_CONFIG.length + 1)
        val message = "Path must not contains ROOT_CONFIG macro, corrected: $path"
        if (ApplicationManager.getApplication().isUnitTestMode) throw AssertionError(message) else LOG.warn(message)
      }
      return path
    }

    override fun pathToFile(path: String): Path {
      return Paths.get(ApplicationManager.getApplication().stateStore.storageManager.expandMacros(ROOT_CONFIG), path)!!
    }
  }

  @Suppress("unused")
  private class ProjectSchemeManagerFactory(private val project: Project) : SchemeManagerFactoryBase() {
    override val componentManager = project

    override fun getVirtualFileResolver() = project as? VirtualFileResolver?

    private fun addVfsListener(schemeManager: SchemeManagerImpl<*, *>) {
      @Suppress("UNCHECKED_CAST")
      project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, SchemeFileTracker(schemeManager as SchemeManagerImpl<Any, Any>, project))
    }

    override fun createFileChangeSubscriber(): FileChangeSubscriber? {
      return { schemeManager ->
        val startupManager = if (ApplicationManager.getApplication().isUnitTestMode) null else StartupManagerEx.getInstanceEx(project)
        if (startupManager == null || startupManager.postStartupActivityPassed()) {
          addVfsListener(schemeManager)
        }
        else {
          startupManager.registerPostStartupActivity(DumbAwareRunnable { addVfsListener(schemeManager) })
        }
      }
    }

    override fun pathToFile(path: String): Path {
      val projectFileDir = (project.stateStore as? IProjectStore)?.projectConfigDir
      if (projectFileDir == null) {
        return Paths.get(project.basePath, ".$path")
      }
      else {
        return Paths.get(projectFileDir, path)
      }
    }
  }

  @TestOnly
  class TestSchemeManagerFactory(private val basePath: Path) : SchemeManagerFactoryBase() {
    override fun pathToFile(path: String) = basePath.resolve(path)!!
  }
}