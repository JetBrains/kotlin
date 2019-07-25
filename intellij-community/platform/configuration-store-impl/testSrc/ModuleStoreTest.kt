// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.impl.stores.BatchUpdateListener
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.impl.storage.ClasspathStorage
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.*
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.io.parentSystemIndependentPath
import com.intellij.util.io.readText
import com.intellij.util.io.systemIndependentPath
import gnu.trove.TObjectIntHashMap
import kotlinx.coroutines.runBlocking
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

const val ESCAPED_MODULE_DIR = "\$MODULE_DIR$"

@RunsInEdt
@RunsInActiveStoreMode
class ModuleStoreTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  private val tempDirManager = TemporaryDirectory()

  @Suppress("unused")
  @JvmField
  @Rule
  val ruleChain = RuleChain(tempDirManager, EdtRule(), ActiveStoreRule(projectRule), DisposeModulesRule(projectRule))

  @Test
  fun `set option`() = runBlocking {
    val moduleFile = runWriteAction {
      VfsTestUtil.createFile(tempDirManager.newVirtualDirectory("module"), "test.iml", """
        <?xml version="1.0" encoding="UTF-8"?>
        <module type="JAVA_MODULE" foo="bar" version="4" />""".trimIndent())
    }

    projectRule.loadModule(moduleFile).useAndDispose {
      assertThat(getOptionValue("foo")).isEqualTo("bar")

      setOption("foo", "not bar")
      stateStore.save()
    }

    projectRule.loadModule(moduleFile).useAndDispose {
      assertThat(getOptionValue("foo")).isEqualTo("not bar")

      setOption("foo", "not bar")
      // ensure that save the same data will not lead to any problems (like "Content equals, but it must be handled not on this level")
      stateStore.save()
    }
  }

  @Test fun `newModule should always create a new module from scratch`() {
    val moduleFile = runWriteAction {
      VfsTestUtil.createFile(tempDirManager.newVirtualDirectory("module"), "test.iml", "<module type=\"JAVA_MODULE\" foo=\"bar\" version=\"4\" />")
    }

    projectRule.createModule(Paths.get(moduleFile.path)).useAndDispose {
      assertThat(getOptionValue("foo")).isNull()
    }
  }

  @Test
  fun `must be empty if classpath storage`() = runBlocking<Unit> {
    // we must not use VFS here, file must not be created
    val moduleFile = tempDirManager.newPath("module", refreshVfs = true).resolve("test.iml")
    projectRule.createModule(moduleFile).useAndDispose {
      ModuleRootModificationUtil.addContentRoot(this, moduleFile.parentSystemIndependentPath)
      stateStore.save()
      assertThat(moduleFile).isRegularFile
      assertThat(moduleFile.readText()).startsWith("""
      <?xml version="1.0" encoding="UTF-8"?>
      <module type="JAVA_MODULE" version="4">""".trimIndent())

      ClasspathStorage.setStorageType(ModuleRootManager.getInstance(this), "eclipse")
      stateStore.save()
      assertThat(moduleFile).isEqualTo("""
      <?xml version="1.0" encoding="UTF-8"?>
      <module classpath="eclipse" classpath-dir="$ESCAPED_MODULE_DIR" type="JAVA_MODULE" version="4" />""")
    }
  }

  @Test
  fun `one batch update session if several modules changed`() = runBlocking<Unit> {
    val nameToCount = TObjectIntHashMap<String>()
    val root = tempDirManager.newPath(refreshVfs = true)

    suspend fun Module.addContentRoot() {
      val moduleName = name
      var batchUpdateCount = 0
      nameToCount.put(moduleName, batchUpdateCount)

      messageBus.connect().subscribe(BatchUpdateListener.TOPIC, object : BatchUpdateListener {
        override fun onBatchUpdateStarted() {
          nameToCount.put(moduleName, ++batchUpdateCount)
        }
      })

      //
      ModuleRootModificationUtil.addContentRoot(this, root.resolve(moduleName).systemIndependentPath)
      assertThat(contentRootUrls).hasSize(1)
      stateStore.save()
    }

    fun removeContentRoot(module: Module) {
      val modulePath = module.stateStore.storageManager.expandMacros(StoragePathMacros.MODULE_FILE)
      val moduleFile = Paths.get(modulePath)
      assertThat(moduleFile).isRegularFile

      val virtualFile = LocalFileSystem.getInstance().findFileByPath(modulePath)!!
      val oldText = moduleFile.readText()
      val newText = oldText.replace("<content url=\"file://\$MODULE_DIR$/${module.name}\" />\n", "")
      assertThat(oldText).isNotEqualTo(newText)
      runWriteAction {
        virtualFile.setBinaryContent(newText.toByteArray())
      }
    }

    fun assertChangesApplied(module: Module) {
      assertThat(module.contentRootUrls).isEmpty()
    }

    val m1 = projectRule.createModule(root.resolve("m1.iml"))
    val m2 = projectRule.createModule(root.resolve("m2.iml"))

    var projectBatchUpdateCount = 0
    projectRule.project.messageBus.connect(m1).subscribe(BatchUpdateListener.TOPIC, object : BatchUpdateListener {
      override fun onBatchUpdateStarted() {
        nameToCount.put("p", ++projectBatchUpdateCount)
      }
    })

    m1.addContentRoot()
    m2.addContentRoot()

    removeContentRoot(m1)
    removeContentRoot(m2)

    StoreReloadManager.getInstance().reloadChangedStorageFiles()

    assertChangesApplied(m1)
    assertChangesApplied(m2)

    assertThat(nameToCount.size()).isEqualTo(3)
    assertThat(nameToCount.get("p")).isEqualTo(1)
    assertThat(nameToCount.get("m1")).isEqualTo(1)
    assertThat(nameToCount.get("m1")).isEqualTo(1)
  }
}

inline fun <T> Module.useAndDispose(task: Module.() -> T): T {
  try {
    return task()
  }
  finally {
    ModuleManager.getInstance(project).disposeModule(this)
  }
}

fun ProjectRule.loadModule(file: VirtualFile): Module {
  val project = project
  return runWriteAction { ModuleManager.getInstance(project).loadModule(file.path) }
}

val Module.contentRootUrls: Array<String>
  get() = ModuleRootManager.getInstance(this).contentRootUrls

internal fun ProjectRule.createModule(path: Path): Module {
  val project = project
  return runWriteAction { ModuleManager.getInstance(project).newModule(path.systemIndependentPath, ModuleTypeId.JAVA_MODULE) }
}