// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.roots.ui.configuration.SdkComboBoxModel.Companion.createJdkComboBoxModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.Disposer
import java.util.function.Predicate

abstract class SdkComboBoxTestCase : SdkTestCase() {

  fun createJdkComboBox(): SdkComboBox {
    val sdksModel = TestProjectSdksModel()
    sdksModel.reset(project)
    Disposer.register(testRootDisposable, sdksModel)
    val model = createJdkComboBoxModel(project, sdksModel, Predicate { it is TestSdkType })
    return SdkComboBox(model)
  }

  class ComboBoxChecker(val comboBox: SdkComboBox) :
    CollectionChecker<SdkListItem>(comboBox.itemSequence.iterator(), comboBox.dumpToString()) {

    inline fun <reified I : SdkListItem> item(
      isSelected: Boolean = false,
      noinline assertItem: SdkComboBox.(I) -> Unit = {}
    ): ComboBoxChecker {
      element<I> {
        assertSelection(comboBox, it, isSelected)
        comboBox.assertItem(it)
      }
      return this
    }

    fun assertSelection(comboBox: SdkComboBox, elementToCheck: SdkListItem, mustBeSelected: Boolean) {
      if (mustBeSelected) {
        val message = "'${comboBox.dumpToString(elementToCheck)}' must be selected"
        assertTrue(message, comboBox.selectedItem == elementToCheck)
      }
      else {
        val message = "'${comboBox.dumpToString(elementToCheck)}' must not be selected"
        assertFalse(message, comboBox.selectedItem == elementToCheck)
      }
    }
  }

  open class CollectionChecker<T : Any>(val iterator: Iterator<T?>, val dump: String) {

    inline fun <reified I : T> element(noinline assertItem: (I) -> Unit = {}): CollectionChecker<T> {
      assertTrue(dump, iterator.hasNext())
      val element = iterator.next()
      val item = assertIsInstance<I>(dump, element)
      withMessageIfException { assertItem(item) }
      return this
    }

    fun nothing() {
      assertFalse(dump, iterator.hasNext())
    }

    fun <R> withMessageIfException(action: () -> R): R {
      try {
        return action()
      }
      catch (ex: Throwable) {
        System.err.println("${ex::class.java.name}: $dump")
        throw ex
      }
    }
  }

  /**
   * Works in team with:
   *  [SdkConfigurationUtil.selectSdkHome(SdkType, Component, Consumer<in String>)],
   *  [SdkComboBoxTestCase.TestProjectSdksModel]
   */
  object CanarySdk : TestSdk("canary", "canary-home", "canary-version", TestSdkType) {
    fun <R> replaceByTestSdk(action: () -> R): R {
      return invokeAndWaitIfNeeded {
        runWriteAction {
          val projectSdkTable = ProjectJdkTable.getInstance()
          projectSdkTable.addJdk(CanarySdk)
          try {
            action()
          }
          finally {
            projectSdkTable.removeJdk(CanarySdk)
          }
        }
      }
    }
  }

  class TestProjectSdksModel : ProjectSdksModel(), Disposable {
    override fun addSdk(type: SdkType, home: String, callback: com.intellij.util.Consumer<in Sdk>?) {
      if (home == CanarySdk.homePath) {
        val sdk = TestSdkGenerator.createNextSdk()
        setupSdk(sdk, callback)
      }
      else {
        super.addSdk(type, home, callback)
      }
    }

    private fun setupSdk(newJdk: TestSdk, callback: com.intellij.util.Consumer<in Sdk>?) {
      val sdkType = newJdk.sdkType as SdkType
      if (!sdkType.setupSdkPaths(newJdk, this)) return
      doAdd(newJdk, callback)
    }

    override fun reset(project: Project?) {
      disposeEditableSdks()
      super.reset(project)
    }

    override fun dispose() = disposeEditableSdks()

    private fun disposeEditableSdks() {
      for (editable in projectSdks.values) {
        if (editable is Disposable) {
          Disposer.dispose(editable)
        }
      }
    }
  }

  companion object {
    fun assertSdkItem(expected: TestSdk, item: SdkListItem.SdkItem) {
      assertSdk(expected, item.sdk)
    }

    fun assertActionItem(role: SdkListItem.ActionRole, typeId: SdkType, item: SdkListItem.ActionItem) {
      assertEquals(role, item.role)
      assertEquals(typeId, item.action.sdkType)
    }

    inline fun <reified I : SdkListItem> assertComboBoxSelection(comboBox: SdkComboBox, expectedSdk: Sdk?) {
      assertEquals(comboBox.dumpToString(), expectedSdk, comboBox.getSelectedSdk())
      assertIsInstance<I>(comboBox.dumpToString(), comboBox.getSelectedItem())
    }

    fun assertComboBoxContent(comboBox: SdkComboBox) = ComboBoxChecker(comboBox)

    fun assertCollectionContent(collection: Collection<*>) = CollectionChecker(collection.iterator(), collection.toString())

    inline fun <reified T> assertIsInstance(message: String, o: Any?): T {
      requireNotNull(o) { message }
      assertTrue("Expected instance of: ${T::class.java.name} actual: ${o::class.java.name}, $message", o is T)
      return o as T
    }

    fun SdkComboBox.getProjectSdk() = model.sdksModel.projectSdk?.let { TestSdkGenerator.findTestSdk(it) }

    val SdkComboBox.itemSequence
      get() = sequence {
        val model = getModel()
        for (i in 0 until model.getSize()) {
          yield(model.getElementAt(i))
        }
      }

    fun SdkComboBox.touchDownloadAction(): TestSdk {
      selectedItem = itemSequence
        .filterIsInstance<SdkListItem.ActionItem>()
        .first { it.role == SdkListItem.ActionRole.DOWNLOAD }
      return TestSdkGenerator.getCurrentSdk()
    }

    fun SdkComboBox.touchAddAction(): TestSdk {
      CanarySdk.replaceByTestSdk {
        selectedItem = itemSequence
          .filterIsInstance<SdkListItem.ActionItem>()
          .first { it.role == SdkListItem.ActionRole.ADD }
      }
      return TestSdkGenerator.getCurrentSdk()
    }

    fun SdkComboBox.withOpenDropdownPopup(): SdkComboBox {
      firePopupMenuWillBecomeVisible()
      return this
    }

    fun SdkComboBox.dumpToString(): String {
      val elements = itemSequence.joinToString { (if (selectedItem == it) "* " else "") + dumpToString(it) }
      return "ComboBox { $elements }"
    }

    fun SdkComboBox.dumpToString(element: SdkListItem?): String {
      return when (element) {
        is SdkListItem.NoneSdkItem -> "[none]"
        is SdkListItem.ProjectSdkItem -> "[project] ${getProjectSdk()?.name}"
        is SdkListItem.InvalidSdkItem -> "[invalid] ${element.sdkName}"
        is SdkListItem.SdkReferenceItem -> "[reference] ${element.name}"
        is SdkListItem.SdkItem -> "[sdk] ${element.sdk.name}"
        is SdkListItem.SuggestedItem -> "[suggested] ${element.homePath}"
        is SdkListItem.ActionItem -> "[action] ${element.myRole} ${element.myAction.sdkType.name}"
        is SdkListItem.GroupItem -> "[group] {${element.mySubItems.joinToString { dumpToString(it) }}}"
        null -> "null"
        else -> element::class.java.name
      }
    }
  }
}