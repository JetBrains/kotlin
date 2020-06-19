// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ProjectTopics
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManagerEx
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.*
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.Function
import com.intellij.util.SmartList
import com.intellij.util.io.Ksuid
import com.intellij.util.io.readText
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.nio.file.Path
import kotlin.properties.Delegates

private val Module.storage: FileBasedStorage
  get() = (stateStore.storageManager as StateStorageManagerImpl).getCachedFileStorages(listOf(StoragePathMacros.MODULE_FILE)).first()

internal class ModuleStoreRenameTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  var module: Module by Delegates.notNull()
  var dependentModule: Module by Delegates.notNull()

  // we test fireModuleRenamedByVfsEvent
  private val oldModuleNames = SmartList<String>()

  private val tempDirManager = TemporaryDirectory()

  @Rule
  @JvmField
  val ruleChain = RuleChain(
    tempDirManager,
    object : ExternalResource() {
      override fun before() {
        runInEdtAndWait {
          val moduleFileParent = tempDirManager.newPath()
          module = projectRule.createModule(moduleFileParent.resolve("m.iml"))
          dependentModule = projectRule.createModule(moduleFileParent.resolve("dependent-module.iml"))
          ModuleRootModificationUtil.addDependency(dependentModule, module)
        }

        module.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
          override fun modulesRenamed(project: Project, modules: MutableList<Module>, oldNameProvider: Function<Module, String>) {
            assertThat(modules).containsOnly(module)
            oldModuleNames.add(oldNameProvider.`fun`(module))
          }
        })
      }

      // should be invoked after project tearDown
      override fun after() {
        (ApplicationManager.getApplication().stateStore.storageManager as StateStorageManagerImpl).getVirtualFileTracker()!!.remove {
          if (it.storageManager.componentManager == module) {
            throw AssertionError("Storage manager is not disposed, module $module, storage $it")
          }
          false
        }
      }
    },
    DisposeModulesRule(projectRule)
  )

  @After
  fun tearDown() {
    ApplicationManager.getApplication().invokeAndWait {
      val undoManager = UndoManager.getInstance(projectRule.project) as UndoManagerImpl
      undoManager.dropHistoryInTests()
      undoManager.flushCurrentCommandMerger()
    }
  }

  // project structure
  @Test
  fun `rename module using model`() = runBlocking<Unit> {
    saveProjectState()

    val storage = module.storage
    val oldFile = storage.file
    assertThat(oldFile).isRegularFile

    val oldName = module.name
    val newName = "foo"

    withContext(AppUIExecutor.onWriteThread().coroutineDispatchingContext()) {
      projectRule.project.modifyModules { renameModule(module, newName) }
    }
    assertRename(newName, oldFile)
    assertThat(oldModuleNames).containsOnly(oldName)
  }

  // project view
  @Test
  fun `rename module using rename virtual file`() = runBlocking {
    testRenameModule()
  }

  private suspend fun testRenameModule() {
    saveProjectState()
    val storage = module.storage
    val oldFile = storage.file
    assertThat(oldFile).isRegularFile

    val oldName = module.name
    val newName = "foo.dot"
    withContext(AppUIExecutor.onWriteThread().coroutineDispatchingContext()) {
      runWriteAction {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(oldFile.systemIndependentPath)!!
          .rename(null, "$newName${ModuleFileType.DOT_DEFAULT_EXTENSION}")
      }
    }
    assertRename(newName, oldFile)
    assertThat(oldModuleNames).containsOnly(oldName)
  }

  // we cannot test external rename yet, because it is not supported - ModuleImpl doesn't support delete and create events (in case of external change we don't get move event, but get "delete old" and "create new")

  private suspend fun assertRename(newName: String, oldFile: Path) {
    val newFile = module.storage.file
    assertThat(newFile.fileName.toString()).isEqualTo("$newName${ModuleFileType.DOT_DEFAULT_EXTENSION}")
    assertThat(oldFile)
      .doesNotExist()
      .isNotEqualTo(newFile)
    assertThat(newFile).isRegularFile

    // ensure that macro value updated
    assertThat(module.stateStore.storageManager.expandMacros(StoragePathMacros.MODULE_FILE)).isEqualTo(newFile.systemIndependentPath)

    saveProjectState()
    assertThat(dependentModule.storage.file.readText()).contains("""<orderEntry type="module" module-name="$newName" />""")
  }

  @Test
  fun `rename module parent virtual dir`() = runBlocking {
    saveProjectState()
    val storage = module.storage
    val oldFile = storage.file
    val parentVirtualDir = storage.getVirtualFile(StateStorageOperation.WRITE)!!.parent
    withContext(AppUIExecutor.onWriteThread().coroutineDispatchingContext()) {
      runWriteAction {
        parentVirtualDir.rename(null, Ksuid.generate())
      }
    }

    val newFile = parentVirtualDir.toNioPath().resolve("${module.name}${ModuleFileType.DOT_DEFAULT_EXTENSION}")
    assertThat(newFile).isRegularFile
    assertRename(module.name, oldFile)
    assertThat(oldModuleNames).isEmpty()

    testRenameModule()
  }

  @Test
  fun `rename module source root`() = runBlocking<Unit>(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
    saveProjectState()
    val storage = module.storage
    val parentVirtualDir = storage.getVirtualFile(StateStorageOperation.WRITE)!!.parent
    val src = VfsTestUtil.createDir(parentVirtualDir, "foo")
    withContext(AppUIExecutor.onWriteThread().coroutineDispatchingContext()) {
      runWriteAction {
        PsiTestUtil.addSourceContentToRoots(module, src, false)
      }
    }
    saveProjectState()

    val rootManager = module.rootManager as ModuleRootManagerEx
    val stateModificationCount = rootManager.modificationCountForTests

    withContext(AppUIExecutor.onWriteThread().coroutineDispatchingContext()) {
      runWriteAction {
        src.rename(null, "bar.dot")
      }
    }

    assertThat(stateModificationCount).isLessThan(rootManager.modificationCountForTests)
  }

  private suspend fun saveProjectState() {
    projectRule.project.stateStore.save()
  }
}