// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.rules

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.project.stateStore
import com.intellij.testFramework.WrapRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.io.Ksuid
import com.intellij.util.io.delete
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Paths

private const val TEST_COMPONENT_NAME = "DefaultProjectStoreTestComponent"

@State(name = TEST_COMPONENT_NAME, allowLoadInTests = true)
private class TestComponent : SimplePersistentStateComponent<TestComponent.TestComponentState>(TestComponentState()) {
  class TestComponentState : BaseState() {
    @get:Attribute
    var foo by string()
    @get:Attribute
    var bar by string()
  }
}

@ApiStatus.Internal
fun checkDefaultProjectAsTemplate(task: (checkTask: (project: Project, defaultProjectTemplateShouldBeApplied: Boolean) -> Unit) -> Unit) {
  val defaultTestComponent = TestComponent()
  val defaultStateStore = ProjectManager.getInstance().defaultProject.service<IComponentStore>()
  defaultStateStore.initComponent(defaultTestComponent, null, null)
  // must be after init otherwise will be not saved on disk (as will be not modified since init)
  defaultTestComponent.state.foo = Ksuid.generate()
  defaultTestComponent.state.bar = Ksuid.generate()
  try {
    task { project, defaultProjectTemplateShouldBeApplied ->
      val component = TestComponent()
      project.stateStore.initComponent(component, null, null)
      val assertThat = assertThat(component.state)
      if (defaultProjectTemplateShouldBeApplied) {
        assertThat.isEqualTo(defaultTestComponent.state)
      }
      else {
        assertThat.isEqualTo(TestComponent.TestComponentState())
      }
    }
  }
  finally {
    // clear state
    defaultStateStore.removeComponent(TEST_COMPONENT_NAME)
  }
}

@ApiStatus.Internal
fun createDeleteAppConfigRule(): WrapRule {
  return WrapRule {
    val path = Paths.get(ApplicationManager.getApplication().stateStore.storageManager.expandMacros("\$APP_CONFIG$"))
    return@WrapRule {
      path.delete()
    }
  }
}