// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.plugins.gradle.util.GradleAttachSourcesProvider
import org.junit.Test

class GradleAttachSourcesProviderIntegrationTest : GradleImportingTestCase() {
  @Test
  fun `test download sources dynamic task`() {
    importProject(GradleBuildScriptBuilderEx()
                    .withJavaPlugin()
                    .withIdeaPlugin()
                    .withJUnit("4.12")
                    .addPrefix("idea.module.downloadSources = false")
                    .generate())

    assertModules("project", "project.main", "project.test")
    val junitLib: LibraryOrderEntry = getModuleLibDeps("project.test", "Gradle: junit:junit:4.12").single()
    assertThat(junitLib.getRootFiles(OrderRootType.CLASSES))
      .hasSize(1)
      .allSatisfy { assertEquals("junit-4.12.jar", it.name) }

    val psiFile = runReadAction {
      JavaPsiFacade.getInstance(myProject).findClass("junit.framework.Test", GlobalSearchScope.allScope(myProject))!!.containingFile
    }

    val output = mutableListOf<String>()
    val listener = object : ExternalSystemTaskNotificationListenerAdapter() {
      override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        output += text
      }
    }
    try {
      ExternalSystemProgressNotificationManager.getInstance().addNotificationListener(listener)
      val callback = GradleAttachSourcesProvider().getActions(mutableListOf(junitLib), psiFile)
        .single()
        .perform(mutableListOf(junitLib))
        .apply { waitFor(5000) }
      assertNull(callback.error)
    }
    finally {
      ExternalSystemProgressNotificationManager.getInstance().removeNotificationListener(listener)
    }

    assertThat(output)
      .filteredOn{ it.startsWith("Sources were downloaded to") }
      .hasSize(1)
      .allSatisfy { assertThat(it).endsWith("junit-4.12-sources.jar") }

    assertThat(junitLib.getRootFiles(OrderRootType.SOURCES))
      .hasSize(1)
      .allSatisfy { assertEquals("junit-4.12-sources.jar", it.name) }

  }
}