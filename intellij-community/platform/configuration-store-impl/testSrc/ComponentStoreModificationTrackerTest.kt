// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.configurationStore.schemeManager.ROOT_CONFIG
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.ExceptionUtil
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates

@RunsInEdt
internal class ComponentStoreModificationTrackerTest {
  private var testAppConfig: Path by Delegates.notNull()
  private var componentStore: MyComponentStore by Delegates.notNull()

  @JvmField
  @Rule
  val fsRule = InMemoryFsRule()

  @JvmField
  @Rule
  val edtRule = EdtRule()

  @Before
  fun setUp() {
    testAppConfig = fsRule.fs.getPath("/config")
    componentStore = MyComponentStore(testAppConfig)
  }

  @Test
  fun `modification tracker`() = runBlocking<Unit> {
    @State(name = "modificationTrackerA", storages = [(Storage("a.xml"))])
    open class A : PersistentStateComponent<TestState>, SimpleModificationTracker() {
      var options = TestState()

      val stateCalledCount = AtomicLong(0)
      var lastGetStateStackTrace: String? = null

      override fun getState(): TestState {
        lastGetStateStackTrace = ExceptionUtil.currentStackTrace()
        stateCalledCount.incrementAndGet()
        return options
      }

      override fun loadState(state: TestState) {
        this.options = state
      }
    }

    val component = A()
    componentStore.initComponent(component, null)

    assertThat(component.modificationCount).isEqualTo(0)
    assertThat(component.stateCalledCount.get()).isEqualTo(0)

    // test that store correctly set last modification count to component modification count on init
    component.lastGetStateStackTrace = null
    componentStore.save()
    @Suppress("USELESS_CAST")
    assertThat(component.lastGetStateStackTrace as String?).isNull()
    assertThat(component.stateCalledCount.get()).isEqualTo(0)

    // change modification count - store will be forced to check changes using serialization and A.getState will be called
    component.incModificationCount()
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(1)

    // test that store correctly save last modification time and doesn't call our state on next save
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(1)

    val componentFile = testAppConfig.resolve("a.xml")
    assertThat(componentFile).doesNotExist()

    // update data but "forget" to update modification count
    component.options.foo = "new"

    componentStore.save()
    assertThat(componentFile).doesNotExist()

    component.incModificationCount()
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(2)

    assertThat(componentFile).hasContent("""
    <application>
      <component name="modificationTrackerA" foo="new" />
    </application>""".trimIndent())
  }

  @Test
  fun persistentStateComponentWithModificationTracker() = runBlocking<Unit> {
    @State(name = "TestPersistentStateComponentWithModificationTracker", storages = [(Storage("b.xml"))])
    open class A : PersistentStateComponentWithModificationTracker<TestState> {
      var modificationCount = AtomicLong(0)

      override fun getStateModificationCount() = modificationCount.get()

      var options = TestState()

      var stateCalledCount = AtomicLong(0)

      override fun getState(): TestState {
        stateCalledCount.incrementAndGet()
        return options
      }

      override fun loadState(state: TestState) {
        this.options = state
      }

      fun incModificationCount() {
        modificationCount.incrementAndGet()
      }
    }

    val component = A()
    componentStore.initComponent(component, null)

    assertThat(component.modificationCount.get()).isEqualTo(0)
    assertThat(component.stateCalledCount.get()).isEqualTo(0)

    // test that store correctly set last modification count to component modification count on init
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(0)

    // change modification count - store will be forced to check changes using serialization and A.getState will be called
    component.incModificationCount()
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(1)

    // test that store correctly save last modification time and doesn't call our state on next save
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(1)

    val componentFile = testAppConfig.resolve("b.xml")
    assertThat(componentFile).doesNotExist()

    // update data but "forget" to update modification count
    component.options.foo = "new"

    componentStore.save()
    assertThat(componentFile).doesNotExist()

    component.incModificationCount()
    componentStore.save()
    assertThat(component.stateCalledCount.get()).isEqualTo(2)

    assertThat(componentFile).hasContent("""
    <application>
      <component name="TestPersistentStateComponentWithModificationTracker" foo="new" />
    </application>""".trimIndent())
  }

}

private class MyComponentStore(testAppConfigPath: Path) : ChildlessComponentStore() {
  private class MyStorageManager(private val rootDir: Path) : StateStorageManagerImpl("application") {
    override fun getFileBasedStorageConfiguration(fileSpec: String) = appFileBasedStorageConfiguration

    override val isUseXmlProlog = false

    override fun normalizeFileSpec(fileSpec: String) = removeMacroIfStartsWith(super.normalizeFileSpec(fileSpec), APP_CONFIG)

    override fun expandMacros(path: String) = if (path[0] == '$') super.expandMacros(path) else "${expandMacro(APP_CONFIG)}/$path"

    override fun resolvePath(path: String): Path = rootDir.resolve(path)
  }

  override val storageManager = MyStorageManager(testAppConfigPath)

  init {
    setPath(testAppConfigPath.systemIndependentPath)
  }

  override fun setPath(path: String) {
    storageManager.addMacro(APP_CONFIG, path)
    // yes, in tests APP_CONFIG equals to ROOT_CONFIG (as ICS does)
    storageManager.addMacro(ROOT_CONFIG, path)
  }
}