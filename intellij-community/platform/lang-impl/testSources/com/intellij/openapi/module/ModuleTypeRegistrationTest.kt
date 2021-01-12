// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.module.impl.ModuleTypeManagerImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.systemIndependentPath
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.util.ui.EmptyIcon
import java.io.File
import javax.swing.Icon

class ModuleTypeRegistrationTest : HeavyPlatformTestCase() {
  fun `test unregister module type and register again`() {
    val moduleTypeManager = ModuleTypeManager.getInstance()
    val moduleManager = ModuleManager.getInstance(myProject)
    runWithRegisteredType {
      val moduleType = moduleTypeManager.findByID(MockModuleType.ID)
      assertInstanceOf(moduleType, MockModuleType::class.java)
      val module = runWriteAction {
        moduleManager.newModule(File(createTempDirectory(), "test.iml").systemIndependentPath, MockModuleType.ID)
      }
      assertSame(moduleType, ModuleType.get(module))
    }

    val unknownType = moduleTypeManager.findByID(MockModuleType.ID)
    assertInstanceOf(unknownType, UnknownModuleType::class.java)
    val module = moduleManager.findModuleByName("test")!!
    assertInstanceOf(ModuleType.get(module), UnknownModuleType::class.java)

    registerModuleType(testRootDisposable)
    val moduleType = moduleTypeManager.findByID(MockModuleType.ID)
    assertInstanceOf(moduleType, MockModuleType::class.java)
    assertSame(moduleType, ModuleType.get(module))
  }

  private fun runWithRegisteredType(action: () -> Unit) {
    val moduleTypeDisposable = Disposer.newDisposable()
    registerModuleType(moduleTypeDisposable)
    try {
      action()
    }
    finally {
      Disposer.dispose(moduleTypeDisposable)
    }
  }

  private fun registerModuleType(disposable: Disposable) {
    val moduleTypeDisposable = Disposer.newDisposable()
    Disposer.register(disposable, Disposable {
      runWriteAction {
        Disposer.dispose(moduleTypeDisposable)
      }
    })
    val extension = ModuleTypeEP()
    extension.setPluginDescriptor(DefaultPluginDescriptor("test"))
    extension.id = MockModuleType.ID
    extension.implementationClass = MockModuleType::class.qualifiedName
    ModuleTypeManagerImpl.EP_NAME.getPoint().registerExtension(extension, moduleTypeDisposable)
  }
}

private class MockModuleType : ModuleType<ModuleBuilder>("mock") {
  companion object {
    const val ID = "mock"
  }

  override fun createModuleBuilder(): ModuleBuilder {
    throw UnsupportedOperationException()
  }

  override fun getName(): String = "Mock"

  override fun getDescription(): String = ""

  override fun getNodeIcon(isOpened: Boolean): Icon = EmptyIcon.ICON_16
}
