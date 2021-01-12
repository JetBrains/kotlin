// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.EmptyModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.systemIndependentPath
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.util.ui.UIUtil
import java.io.File

class OrderEnumeratorHandlerRegistrationTest : HeavyPlatformTestCase() {
  fun `test unregister order enumeration handler`() {
    val (moduleA, moduleB, moduleC) = runWriteAction {
      listOf("a", "b", "c").map {
        ModuleManager.getInstance(myProject).newModule(File(createTempDirectory(), "$it.iml").systemIndependentPath, EmptyModuleType.EMPTY_MODULE)
      }
    }
    val dummyRoot = VirtualFileManager.getInstance().findFileByUrl("temp:///")!!
    ModuleRootModificationUtil.addDependency(moduleA, moduleB)
    ModuleRootModificationUtil.addDependency(moduleB, moduleC)
    val srcRoot = runWriteAction { dummyRoot.createChildDirectory(this, "project-model").createChildDirectory(this, "src") }
    PsiTestUtil.addSourceRoot(moduleC, srcRoot)
    runWithRegisteredExtension {
      val enumerator = ModuleRootManager.getInstance(moduleA).orderEntries().recursively().roots(OrderRootType.SOURCES).usingCache()
      assertEmpty(enumerator.roots)
    }
    val enumerator = ModuleRootManager.getInstance(moduleA).orderEntries().recursively().roots(OrderRootType.SOURCES).usingCache()
    assertEquals(srcRoot, assertOneElement(enumerator.roots))
  }

  private fun runWithRegisteredExtension(action: () -> Unit) {
    val orderEnumerationDisposable = Disposer.newDisposable()
    registerOrderEnumerationHandler(orderEnumerationDisposable)
    try {
      action()
    }
    finally {
      Disposer.dispose(orderEnumerationDisposable)
    }
  }

  private fun registerOrderEnumerationHandler(disposable: Disposable) {
    val orderEnumerationDisposable = Disposer.newDisposable()
    Disposer.register(disposable, Disposable {
      runWriteAction {
        Disposer.dispose(orderEnumerationDisposable)
      }
      UIUtil.dispatchAllInvocationEvents()
    })
    OrderEnumerationHandler.EP_NAME.getPoint().registerExtension(MockOrderEnumerationHandlerFactory(), orderEnumerationDisposable)
    UIUtil.dispatchAllInvocationEvents()
  }
}

private class MockOrderEnumerationHandlerFactory : OrderEnumerationHandler.Factory() {
  override fun isApplicable(module: Module): Boolean = true

  override fun createHandler(module: Module): OrderEnumerationHandler = MockOrderEnumerationHandler()
}

private class MockOrderEnumerationHandler : OrderEnumerationHandler() {
  override fun shouldProcessDependenciesRecursively(): Boolean = false
}