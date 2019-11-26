// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.junit.Test

class GradleBuildSrcImportingTest : GradleImportingTestCase() {

  @Test
  fun `test buildSrc project is imported as modules`() {
    createProjectSubFile("buildSrc/src/main/java/my/pack/TestPlugin.java",
                         """
                            package my.pack;
                            import org.gradle.api.Project;
                            import org.gradle.api.Plugin;
                            public class TestPlugin implements Plugin<Project> {
                              public void apply(Project project){};
                            }
                            """.trimIndent())
    importProject("apply plugin: 'java'\n" +
                  "apply plugin: my.pack.TestPlugin")
    assertModules("project", "project.main", "project.test",
                  "project.buildSrc", "project.buildSrc.main", "project.buildSrc.test")
  }
}