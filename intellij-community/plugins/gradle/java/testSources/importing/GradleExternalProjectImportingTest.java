// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalTask;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames;
import org.junit.Test;

/**
 * @author Vladislav.Soroka
 */
public class GradleExternalProjectImportingTest extends GradleImportingTestCase {

  @Test
  public void testDummyJarTask() throws Exception {
    importProject(
      "task myJar(type: Jar)"
    );

    assertModules("project");

    ExternalProject externalProject = ExternalProjectDataCache.getInstance(myProject).getRootExternalProject(getProjectPath());
    ExternalTask task = externalProject.getTasks().get("myJar");
    assertEquals(":myJar", task.getQName());

    assertEquals(GradleCommonClassNames.GRADLE_API_TASKS_BUNDLING_JAR, task.getType());
  }

  @Test
  public void testProjectImportUsingNonRootProjectPath() throws Exception {
    createProjectSubFile("../settings.gradle", "rootProject.name = 'root'\n" +
                                               "include 'project', 'another_project'");
    createProjectSubFile("../build.gradle", "allprojects { apply plugin: 'java' }");
    importProject("");

    assertModules("root", "root.main", "root.test",
                  "root.project", "root.project.main", "root.project.test",
                  "root.another_project", "root.another_project.main", "root.another_project.test");

    ExternalProject externalProject = ExternalProjectDataCache.getInstance(myProject).getRootExternalProject(getProjectPath());
    assertEquals("root", externalProject.getName());
  }
}
