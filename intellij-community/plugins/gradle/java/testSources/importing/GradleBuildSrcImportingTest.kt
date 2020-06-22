// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
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

  @Test
  fun `test buildSrc project level dependencies are imported`() {
    createProjectSubFile("buildSrc/build.gradle", GradleBuildScriptBuilderEx().withJUnit("4.12").generate())
    importProject("")
    assertModules("project",
                  "project.buildSrc", "project.buildSrc.main", "project.buildSrc.test")
    val moduleLibDeps = getModuleLibDeps("project.buildSrc.test", "Gradle: junit:junit:4.12")
    assertThat(moduleLibDeps).hasSize(1).allSatisfy {
      assertThat(it.libraryLevel).isEqualTo("project")
    }
  }

  @TargetVersions("<6.0") // since 6.0 'buildSrc' is a reserved project name, https://docs.gradle.org/current/userguide/upgrading_version_5.html#buildsrc_is_now_reserved_as_a_project_and_subproject_build_name
  @Test
  fun `test buildSrc project is included into the main build`() {
    createProjectSubFile("buildSrc/src/main/java/my/pack/Util.java",
                         "package my.pack;\npublic class Util {}")

    importProject("apply plugin: 'java'")
    assertModules("project", "project.main", "project.test",
                  "project.buildSrc", "project.buildSrc.main", "project.buildSrc.test")

    createSettingsFile("include 'buildSrc'")
    importProject("apply plugin: 'java'")
    assertModules("project", "project.main", "project.test", "project.buildSrc")

  }
}