// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.build.BuildTreeConsoleView
import com.intellij.testFramework.PlatformTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.junit.Test

class GradleOutputParsersMessagesImportingTest : SyncViewMessagesImportingTestCase() {

  @Test
  fun `test Gradle build script errors`() {
    createSettingsFile("include 'api', 'impl' ")
    createProjectSubFile("impl/build.gradle",
                         "dependencies {\n" +
                         "   ghostConf project(':api')\n" +
                         "}")
    importProject("subprojects { apply plugin: 'java' }")
    assertThat(syncViewManager.buildsMap).hasSize(1)

    val buildView = syncViewManager.buildsMap.values.first()
    val eventView = buildView.getView(BuildTreeConsoleView::class.java.name, BuildTreeConsoleView::class.java)
    eventView!!.addFilter { true }

    edt {
      val tree = eventView.tree
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)

      val executionTreeText: String
      when {
        currentGradleVersion < GradleVersion.version("2.14") -> executionTreeText =
          "-\n" +
          " -failed\n" +
          "  -impl/build.gradle\n"+
          "   Could not find method ghostConf() for arguments [project ':api'] on project ':impl'"
        else -> executionTreeText =
          "-\n" +
          " -failed\n" +
          "  -impl/build.gradle\n"+
          "   Could not find method ghostConf() for arguments [project ':api'] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler"
      }

      PlatformTestUtil.assertTreeEqual(tree, executionTreeText)
    }
  }
}
