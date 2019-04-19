// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.configurationStore

import com.intellij.configurationStore.ESCAPED_MODULE_DIR
import com.intellij.configurationStore.useAndDispose
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.impl.inWriteAction
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl
import com.intellij.project.stateStore
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.RunsInActiveStoreMode
import com.intellij.testFramework.TemporaryDirectory
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.createProjectAndUseInLoadComponentStateMode
import com.intellij.util.io.delete
import com.intellij.util.io.parentSystemIndependentPath
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Paths

@RunsInActiveStoreMode
class ExternalSystemStorageTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @JvmField
  @Rule
  val tempDirManager = TemporaryDirectory()

  @Test
  fun `must be empty if external system storage`() = runBlocking {
    createProjectAndUseInLoadComponentStateMode(tempDirManager, directoryBased = true) { project ->
      ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)

      val dotIdeaDir = Paths.get(project.stateStore.directoryStorePath)

      val cacheDir = ExternalProjectsDataStorage.getProjectConfigurationDir(project).resolve("modules")
      cacheDir.delete()

      // we must not use VFS here, file must not be created
      val moduleFile = dotIdeaDir.parent.resolve("test.iml")
      withContext(AppUIExecutor.onUiThread().inWriteAction().coroutineDispatchingContext()) {
        ModuleManager.getInstance(project).newModule(moduleFile.systemIndependentPath, ModuleTypeId.JAVA_MODULE)
      }
        .useAndDispose {
          assertThat(cacheDir).doesNotExist()

          ModuleRootModificationUtil.addContentRoot(this, moduleFile.parentSystemIndependentPath)

          stateStore.save()
          assertThat(cacheDir).doesNotExist()
          assertThat(moduleFile).isEqualTo("""
      <?xml version="1.0" encoding="UTF-8"?>
      <module type="JAVA_MODULE" version="4">
        <component name="NewModuleRootManager" inherit-compiler-output="true">
          <exclude-output />
          <content url="file://$ESCAPED_MODULE_DIR" />
          <orderEntry type="sourceFolder" forTests="false" />
        </component>
      </module>""")

          ExternalSystemModulePropertyManager.getInstance(this).setMavenized(true)
          // force re-save: this call not in the setMavenized because ExternalSystemModulePropertyManager in the API (since in production we have the only usage, it is ok for now)
          (ModuleRootManager.getInstance(this) as ModuleRootManagerImpl).stateChanged()

          assertThat(cacheDir).doesNotExist()
          stateStore.save()
          assertThat(cacheDir).isDirectory
          assertThat(moduleFile).isEqualTo("""
      <?xml version="1.0" encoding="UTF-8"?>
      <module type="JAVA_MODULE" version="4" />""")

          assertThat(cacheDir.resolve("test.xml")).isEqualTo("""
      <module>
        <component name="ExternalSystem" externalSystem="Maven" />
        <component name="NewModuleRootManager" inherit-compiler-output="true">
          <exclude-output />
          <content url="file://$ESCAPED_MODULE_DIR" />
          <orderEntry type="sourceFolder" forTests="false" />
        </component>
      </module>""")

          assertThat(dotIdeaDir.resolve("modules.xml")).doesNotExist()
        }
    }
  }
}