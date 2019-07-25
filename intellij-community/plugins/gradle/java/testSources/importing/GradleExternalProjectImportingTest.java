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
}
