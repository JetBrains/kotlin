// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import org.junit.Test;

import java.io.IOException;

public class GradleJpsJavaCompilationTest extends GradleJpsCompilingTestCase {
  @Test
  public void testCustomSourceSetDependencies() throws IOException {
    ExternalProjectsManagerImpl.getInstance(myProject).setStoreExternally(true);
    createProjectSubFile("src/intTest/java/DepTest.java", "class DepTest extends CommonTest {}");
    createProjectSubFile("src/test/java/CommonTest.java", "public class CommonTest {}");
    importProject("apply plugin: 'java'\n" +
                  "sourceSets {\n" +
                  "  intTest {\n" +
                  "     compileClasspath += main.output + test.output" +
                  "  }\n" +
                  "}");
    compileModules("project.main", "project.test", "project.intTest");
  }

  @Override
  protected boolean useDirectoryBasedStorageFormat() {
    return true;
  }
}
