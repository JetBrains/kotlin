// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.builder.FailingTestModelBuilder
import org.junit.Test

class GradleFailingModelBuilderImportingTest : BuildViewMessagesImportingTestCase() {
  override fun setUp() {
    super.setUp()
    GradleProjectResolverExtension.EP_NAME.point.registerExtension(TestFailingModelBuilderProjectResolver(), testRootDisposable)
  }

  @Test
  fun `test simple project`() {
    importProject("")
    if (currentGradleVersion < GradleVersion.version("2.14.1")) {
      assertSyncViewTreeEquals(
        "-\n" +
        " finished"
      )
    }
    else {
      assertSyncViewTreeEquals(
        "-\n" +
        " -finished\n" +
        "  root project 'project': Boom! '\"{}}\n\t"
      )
      assertSyncViewSelectedNode("root project 'project': Boom! '\"{}}\n\t", false) {
        assertThat(it).startsWith("Unable to import Test model\n" +
                                  "\n" +
                                  "java.lang.RuntimeException: Boom! '\"{}}\n\t\n" +
                                  "\tat org.jetbrains.plugins.gradle.tooling.builder.FailingTestModelBuilder.buildAll(FailingTestModelBuilder.java:")
      }
    }
  }
}

class TestFailingModelBuilderProjectResolver : AbstractProjectResolverExtension() {
  override fun getToolingExtensionsClasses(): Set<Class<*>> = setOf(FailingTestModelBuilder.Model::class.java)
  override fun getExtraProjectModelClasses(): Set<Class<*>> = setOf(FailingTestModelBuilder.Model::class.java)
}
