// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.build.BuildTreeConsoleView
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.lang.JavaVersion
import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest
import org.junit.Test
import org.junit.runners.Parameterized

class UnsupportedGradleImportingTest : SyncViewMessagesImportingTestCase() {

  @Test
  fun testSyncMessages() {
    importProject("")
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
        currentGradleVersion < GradleVersion.version("1.0") -> executionTreeText =
          "-\n" +
          " -failed\n" +
          "  Support for builds using Gradle versions older than 2.6 was removed"
        currentGradleVersion < GradleVersion.version("2.6") -> executionTreeText =
          "-\n" +
          " -failed\n" +
          "  Support for builds using Gradle versions older than 2.6 was removed in tooling API version 5.0"
        JavaVersion.current().feature > 8 && currentGradleVersion < GradleVersion.version("3.0") -> executionTreeText =
          "-\n" +
          " -failed\n" +
          "  Cannot determine classpath for resource 'java/sql/SQLException.class' from location 'jrt:/java.sql/java/sql/SQLException.class'"
        else -> executionTreeText = "-\n" +
                                    " successful"
      }

      PlatformTestUtil.assertTreeEqual(tree, executionTreeText)
    }
  }

  override fun assumeTestJavaRuntime(javaRuntimeVersion: JavaVersion) {
    // run on all Java Runtime
  }

  companion object {
    private val OLD_GRADLE_VERSIONS = arrayOf(
      arrayOf<Any>("0.7"), /*arrayOf<Any>("0.8"), arrayOf<Any>("0.9"), ..., */arrayOf<Any>("0.9.2"),
      arrayOf<Any>("1.0"), /*arrayOf<Any>("1.1"), arrayOf<Any>("1.2"), ..., */arrayOf<Any>("1.12"),
      arrayOf<Any>("2.0"), /*arrayOf<Any>("2.1"), arrayOf<Any>("2.2"), ..., */arrayOf<Any>("2.5"))

    /**
     * Run the test against very old not-supported Gradle versions also
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Array<out Array<Any>>? {
      return OLD_GRADLE_VERSIONS + AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS
    }
  }
}
