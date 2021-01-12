// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform

import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.project.stateStore
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.TemporaryDirectory
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.createOrLoadProject
import com.intellij.util.io.createDirectories
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Paths
import kotlin.properties.Delegates

internal class ModuleAttachProcessorTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val tempDirManager = TemporaryDirectory()

  @Test
  fun `attach with iml`() = runBlocking {
    var existingProjectDir: String by Delegates.notNull()
    createOrLoadProject(tempDirManager) { existingProject ->
      existingProjectDir = existingProject.basePath!!
      withContext(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
        runWriteAction {
          ModuleManager.getInstance(existingProject).newModule("$existingProjectDir/test.iml", ModuleTypeId.WEB_MODULE)
        }
        existingProject.stateStore.save()
      }
    }

    createOrLoadProject(tempDirManager) { currentProject ->
      currentProject.stateStore.save()
      withContext(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
        assertThat(ModuleAttachProcessor().attachToProject(currentProject, Paths.get(existingProjectDir), null)).isTrue()
      }
    }
  }

  @Test
  fun `attach without iml`() = runBlocking {
    createOrLoadProject(tempDirManager) { currentProject ->
      currentProject.stateStore.save()
      val existingProjectDir = tempDirManager.newPath().createDirectories()
      runInEdt {
        assertThat(ModuleAttachProcessor().attachToProject(currentProject, existingProjectDir, null)).isTrue()
      }
    }
  }
}